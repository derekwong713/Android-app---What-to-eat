-- Additive, app-prefixed schema. Apply in a transaction; no existing tables are dropped.
begin;
create schema if not exists eat_private;
revoke all on schema eat_private from public, anon, authenticated;
create table public.eat_profiles (
 id uuid primary key references auth.users(id) on delete cascade,
 nickname text not null check(char_length(btrim(nickname)) between 1 and 40 and position('@' in nickname)=0)
);
create table public.eat_cuisines (id text primary key check(id ~ '^[a-z0-9_:-]{1,48}$'),label text not null check(char_length(label) between 1 and 80));
insert into public.eat_cuisines values ('chinese','中式'),('cantonese','粵菜'),('japanese','日式'),('korean','韓式'),('noodles','粉麵'),('thai','泰式'),('italian','意式'),('western','西式'),('indian','印度菜'),('vietnamese','越南菜'),('vegetarian','素食'),('cafe','咖啡輕食');
create table public.eat_restaurants (
 id uuid primary key default gen_random_uuid(), name text not null check(char_length(btrim(name)) between 1 and 120),
 address text not null default '' check(char_length(address)<=500), latitude double precision, longitude double precision,
 opening_hours text check(char_length(opening_hours)<=1000), status text not null default 'unknown' check(status in ('unknown','operating','temporarily_closed','closed')),
 enabled boolean not null default true, source text not null default 'OSM', updated_at timestamptz not null default now(),version bigint not null default 1 check(version>0),
 check((latitude is null)=(longitude is null) and (latitude is null or (latitude between -90 and 90 and longitude between -180 and 180)))
);
create index eat_restaurants_location on public.eat_restaurants(latitude,longitude) where enabled and status in ('unknown','operating');
create table public.eat_restaurant_cuisines (
 restaurant_id uuid not null references public.eat_restaurants(id) on delete cascade,
 cuisine_id text not null references public.eat_cuisines(id),primary key(restaurant_id,cuisine_id)
);
create index eat_cuisine_restaurants on public.eat_restaurant_cuisines(cuisine_id,restaurant_id);
create table public.eat_ratings (
 restaurant_id uuid not null references public.eat_restaurants(id) on delete cascade,user_id uuid not null references auth.users(id) on delete cascade,
 stars integer not null check(stars between 1 and 5), hidden boolean not null default false,updated_at timestamptz not null default now(),primary key(restaurant_id,user_id)
);
create index eat_visible_ratings on public.eat_ratings(restaurant_id) where not hidden;
create table public.eat_comments (
 id uuid primary key default gen_random_uuid(),restaurant_id uuid not null references public.eat_restaurants(id) on delete cascade,
 user_id uuid not null references auth.users(id) on delete cascade,body text not null check(char_length(btrim(body)) between 1 and 2000),
 request_id uuid not null,created_at timestamptz not null default now(),hidden boolean not null default false,unique(user_id,request_id)
);
create index eat_comments_page on public.eat_comments(restaurant_id,created_at desc,id desc) where not hidden;
create table public.eat_revisions (
 id bigint generated always as identity primary key,restaurant_id uuid not null references public.eat_restaurants(id) on delete cascade,
 actor_id uuid references auth.users(id) on delete set null,created_at timestamptz not null default now(),version bigint not null,
 before_data jsonb not null,after_data jsonb not null,origin text not null,unique(restaurant_id,version)
);
create index eat_revisions_page on public.eat_revisions(restaurant_id,version desc);
create table public.eat_reports (
 id uuid primary key default gen_random_uuid(),reporter_id uuid references auth.users(id) on delete set null,
 restaurant_id uuid references public.eat_restaurants(id) on delete cascade,comment_id uuid references public.eat_comments(id) on delete cascade,
 reason text not null check(char_length(btrim(reason)) between 1 and 1000),created_at timestamptz not null default now(),
 resolved_at timestamptz,resolved_by uuid references auth.users(id) on delete set null,resolution text,
 check((restaurant_id is not null)::integer+(comment_id is not null)::integer=1)
);
create index eat_reports_page on public.eat_reports(created_at desc,id desc) where resolved_at is null;
create table eat_private.admins(user_id uuid primary key references auth.users(id) on delete cascade);
create table eat_private.rate_buckets(user_id uuid not null references auth.users(id) on delete cascade,action text not null,bucket timestamptz not null,hits integer not null,primary key(user_id,action,bucket));
create table eat_private.catalog_state(singleton boolean primary key default true check(singleton),version bigint not null default 1);
insert into eat_private.catalog_state values(true,1);
create table eat_private.overrides(restaurant_id uuid primary key references public.eat_restaurants(id) on delete cascade,fields text[] not null default '{}');
create table eat_private.snapshots(id uuid primary key,token_hash text not null,expires_at timestamptz not null,catalog_version bigint not null,cuisines jsonb not null,restaurant_count integer not null,unclassified_count integer not null);
create index eat_snapshot_expiry on eat_private.snapshots(expires_at);
create table eat_private.snapshot_candidates(snapshot_id uuid not null references eat_private.snapshots(id) on delete cascade,cuisine_id text not null,restaurant_id uuid not null,primary key(snapshot_id,cuisine_id,restaurant_id));
create table eat_private.draws(id uuid primary key,token_hash text not null,payload jsonb not null,expires_at timestamptz not null);
create index eat_draw_expiry on eat_private.draws(expires_at);

create function eat_private.require_social() returns uuid language plpgsql security definer set search_path='' as $$
declare u uuid:=auth.uid();
begin
 if u is null or not exists(select 1 from auth.users where id=u) then raise exception 'auth_required';end if;
 if coalesce(auth.jwt()->'app_metadata'->>'provider','') not in ('google','facebook') then raise exception 'forbidden_provider';end if;
 insert into public.eat_profiles(id,nickname) values(u,'食客-'||left(u::text,6)) on conflict do nothing;
 return u;
end $$;
create function eat_private.limit_write(p_action text,p_limit integer) returns void language plpgsql security definer set search_path='' as $$
declare u uuid:=eat_private.require_social();n integer;
begin
 insert into eat_private.rate_buckets values(u,p_action,date_trunc('hour',now()),1)
 on conflict(user_id,action,bucket) do update set hits=eat_private.rate_buckets.hits+1 returning hits into n;
 if n>p_limit then raise exception 'rate_limited';end if;
end $$;
create function public.eat_is_admin() returns boolean language sql stable security definer set search_path='' as $$select exists(select 1 from eat_private.admins where user_id=auth.uid())$$;
create function eat_private.require_admin() returns uuid language plpgsql security definer set search_path='' as $$
declare u uuid:=eat_private.require_social();begin if not public.eat_is_admin() then raise exception 'forbidden';end if;return u;end $$;
create function eat_private.bump_catalog() returns trigger language plpgsql security definer set search_path='' as $$
begin update eat_private.catalog_state set version=version+1 where singleton;return null;end $$;
create trigger eat_catalog_restaurant after insert or update or delete on public.eat_restaurants for each statement execute function eat_private.bump_catalog();
create trigger eat_catalog_cuisine after insert or update or delete on public.eat_restaurant_cuisines for each statement execute function eat_private.bump_catalog();
create function eat_private.distance_m(a double precision,b double precision,c double precision,d double precision) returns double precision language sql immutable set search_path='' as $$
select 6371000.0*2*asin(sqrt(least(1.0,greatest(0.0,power(sin(radians(c-a)/2),2)+cos(radians(a))*cos(radians(c))*power(sin(radians(d-b)/2),2)))))$$;
create function eat_private.restaurant_json(p_id uuid) returns jsonb language sql stable security definer set search_path='' as $$
select jsonb_build_object('id',r.id,'name',r.name,'address',r.address,'latitude',r.latitude,'longitude',r.longitude,'opening_hours',r.opening_hours,'status',r.status,'enabled',r.enabled,'source',r.source,'updated_at',r.updated_at,'version',r.version,
'cuisines',coalesce((select jsonb_agg(jsonb_build_object('id',c.id,'label',c.label) order by c.id) from public.eat_restaurant_cuisines rc join public.eat_cuisines c on c.id=rc.cuisine_id where rc.restaurant_id=r.id),'[]'::jsonb),
'average_rating',(select round(avg(stars),2) from public.eat_ratings where restaurant_id=r.id and not hidden),'rating_count',(select count(*) from public.eat_ratings where restaurant_id=r.id and not hidden)) from public.eat_restaurants r where r.id=p_id and r.enabled$$;
create function public.eat_list_cuisines() returns jsonb language plpgsql stable security definer set search_path='' as $$
begin
 if (select count(*) from public.eat_cuisines)>1000 then raise exception 'catalog_too_large';end if;
 return coalesce((select jsonb_agg(jsonb_build_object('id',id,'label',label) order by id) from public.eat_cuisines),'[]'::jsonb);
end $$;
create function public.eat_create_snapshot(p_latitude double precision default null,p_longitude double precision default null,p_radius integer default null,p_cuisines text[] default '{}') returns jsonb language plpgsql security definer set search_path='' as $$
declare sid uuid:=gen_random_uuid();cap text:=replace(gen_random_uuid()::text||gen_random_uuid()::text,'-','');cv bigint;tags jsonb;rcount integer;ucount integer;ids uuid[];
begin
 if p_radius is not null and (p_radius not in (500,1000,2000,5000) or p_latitude is null or p_longitude is null or not(p_latitude between -90 and 90 and p_longitude between -180 and 180)) then raise exception 'invalid_input_location';end if;
 if cardinality(p_cuisines)>1000 or exists(select 1 from unnest(p_cuisines) x where not exists(select 1 from public.eat_cuisines c where c.id=x)) then raise exception 'invalid_input_cuisine';end if;
 delete from eat_private.draws where expires_at<now();delete from eat_private.snapshots where expires_at<now();
 if (select count(*) from eat_private.snapshots)>1000 then raise exception 'service_busy';end if;
 select version into cv from eat_private.catalog_state where singleton for share;
 select coalesce(array_agg(r.id),'{}'::uuid[]) into ids from public.eat_restaurants r where r.enabled and r.status in ('unknown','operating') and
 (p_radius is null or (r.latitude between p_latitude-p_radius/111000.0 and p_latitude+p_radius/111000.0 and r.longitude between p_longitude-p_radius/(111000.0*greatest(0.01,abs(cos(radians(p_latitude))))) and p_longitude+p_radius/(111000.0*greatest(0.01,abs(cos(radians(p_latitude))))) and eat_private.distance_m(p_latitude,p_longitude,r.latitude,r.longitude)<=p_radius));
 -- This is a complete matching set; above the cap fail explicitly rather than sample a prefix.
 if cardinality(ids)>50000 then raise exception 'catalog_too_large';end if;
 select count(*) into ucount from unnest(ids) x where not exists(select 1 from public.eat_restaurant_cuisines where restaurant_id=x);
 insert into eat_private.snapshots values(sid,encode(sha256(convert_to(cap,'UTF8')),'hex'),now()+interval '10 minutes',cv,'[]',0,ucount);
 insert into eat_private.snapshot_candidates select distinct sid,rc.cuisine_id,rc.restaurant_id from public.eat_restaurant_cuisines rc where rc.restaurant_id=any(ids) and (cardinality(p_cuisines)=0 or rc.cuisine_id=any(p_cuisines));
 select count(distinct restaurant_id) into rcount from eat_private.snapshot_candidates where snapshot_id=sid;
 select coalesce(jsonb_agg(jsonb_build_object('id',x.cuisine_id,'label',c.label,'count',x.n) order by x.cuisine_id),'[]'::jsonb) into tags from (select cuisine_id,count(*) n from eat_private.snapshot_candidates where snapshot_id=sid group by cuisine_id) x join public.eat_cuisines c on c.id=x.cuisine_id;
 update eat_private.snapshots set cuisines=tags,restaurant_count=rcount where id=sid;
 return jsonb_build_object('id',sid,'token',cap,'expires_at',floor(extract(epoch from now()+interval '10 minutes')*1000),'cuisines',tags,'restaurant_count',rcount,'unclassified_count',ucount);
end $$;
create function public.eat_prepare_draw(p_snapshot_id uuid,p_token text,p_draw_id uuid,p_mode text,p_previous_cuisine text default null,p_previous_restaurant uuid default null) returns jsonb language plpgsql security definer set search_path='' as $$
declare s eat_private.snapshots%rowtype;old eat_private.draws%rowtype;cap text:=encode(sha256(convert_to(p_token,'UTF8')),'hex');cv bigint;cs text[];rs uuid[];chosen_c text;chosen_r uuid;n integer;payload jsonb;
begin
 select * into old from eat_private.draws where id=p_draw_id;
 if found then if old.token_hash is distinct from cap or old.expires_at<now() then raise exception 'forbidden';end if;return old.payload;end if;
 select * into s from eat_private.snapshots where id=p_snapshot_id;
 if not found or s.expires_at<now() then raise exception 'snapshot_expired';end if;
 if s.token_hash is distinct from cap then raise exception 'forbidden';end if;
 select version into cv from eat_private.catalog_state where singleton for share;
 if cv<>s.catalog_version then raise exception 'snapshot_changed';end if;
 select array_agg(distinct cuisine_id order by cuisine_id) into cs from eat_private.snapshot_candidates where snapshot_id=s.id;
 if coalesce(cardinality(cs),0)=0 then raise exception 'not_found';end if;
 if p_mode='same_cuisine' then
  if p_previous_cuisine is null or not(p_previous_cuisine=any(cs)) then raise exception 'not_found_cuisine';end if;chosen_c:=p_previous_cuisine;
 elsif p_mode='new_cuisine' then
  if cardinality(cs)>1 and p_previous_cuisine=any(cs) then cs:=array_remove(cs,p_previous_cuisine);end if;
  chosen_c:=cs[case when cardinality(cs)=1 then 1 else floor(random()*cardinality(cs))::integer+1 end];
 else raise exception 'invalid_input_mode';end if;
 select array_agg(distinct restaurant_id order by restaurant_id) into rs from eat_private.snapshot_candidates where snapshot_id=s.id and cuisine_id=chosen_c;
 n:=cardinality(rs);if n>1 and p_previous_restaurant=any(rs) then rs:=array_remove(rs,p_previous_restaurant);end if;
 chosen_r:=rs[case when cardinality(rs)=1 then 1 else floor(random()*cardinality(rs))::integer+1 end];
 payload:=jsonb_build_object('draw_id',p_draw_id,'cuisine_id',chosen_c,'restaurant',eat_private.restaurant_json(chosen_r),'restaurant_count',n);
 insert into eat_private.draws values(p_draw_id,cap,payload,now()+interval '24 hours') on conflict do nothing;
 select * into old from eat_private.draws where id=p_draw_id;
 if old.token_hash is distinct from cap then raise exception 'forbidden';end if;
 return old.payload;
end $$;
create function public.eat_restaurant_detail(p_id uuid,p_before timestamptz default null,p_before_id uuid default null) returns jsonb language plpgsql stable security definer set search_path='' as $$
declare r jsonb;reviews jsonb;begin
 r:=eat_private.restaurant_json(p_id);if r is null then raise exception 'not_found';end if;
 select coalesce(jsonb_agg(x.data order by x.created_at desc,x.id desc),'[]'::jsonb) into reviews from
 (select c.id,c.created_at,jsonb_build_object('id',c.id,'body',c.body,'created_at',c.created_at,'nickname',coalesce(p.nickname,'已刪除帳戶'),'own',coalesce(c.user_id=auth.uid(),false)) data
 from public.eat_comments c left join public.eat_profiles p on p.id=c.user_id where c.restaurant_id=p_id and not c.hidden and (p_before is null or (c.created_at,c.id)<(p_before,p_before_id)) order by c.created_at desc,c.id desc limit 10) x;
 return jsonb_build_object('restaurant',r,'reviews',reviews,'own_rating',(select stars from public.eat_ratings where restaurant_id=p_id and user_id=auth.uid()));
end $$;
create function public.eat_edit_restaurant(p_id uuid,p_expected_version bigint,p_patch jsonb) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=eat_private.require_social();r public.eat_restaurants%rowtype;b jsonb;a jsonb;k text;changed text[]:='{}';tags text[];
begin
 perform eat_private.limit_write('edit',20);
 if p_patch is null or jsonb_typeof(p_patch)<>'object' or exists(select 1 from jsonb_object_keys(p_patch) x where x not in ('name','address','latitude','longitude','cuisine_ids','opening_hours','status')) then raise exception 'invalid_input_fields';end if;
 select * into r from public.eat_restaurants where id=p_id and enabled for update;
 if not found then raise exception 'not_found';end if;if p_expected_version is null or r.version<>p_expected_version then raise exception 'version_conflict';end if;
 b:=eat_private.restaurant_json(p_id);
 if p_patch?'name' then r.name:=btrim(p_patch->>'name');end if;
 if p_patch?'address' then r.address:=btrim(p_patch->>'address');end if;
 if p_patch?'latitude' then r.latitude:=(p_patch->>'latitude')::double precision;end if;
 if p_patch?'longitude' then r.longitude:=(p_patch->>'longitude')::double precision;end if;
 if p_patch?'opening_hours' then r.opening_hours:=nullif(btrim(p_patch->>'opening_hours'),'');end if;
 if p_patch?'status' then r.status:=p_patch->>'status';end if;
 if p_patch?'cuisine_ids' then
  if jsonb_typeof(p_patch->'cuisine_ids')<>'array' or jsonb_array_length(p_patch->'cuisine_ids')>32 then raise exception 'invalid_input_cuisine';end if;
  select coalesce(array_agg(distinct x),'{}') into tags from jsonb_array_elements_text(p_patch->'cuisine_ids') x;
  if exists(select 1 from unnest(tags) x where not exists(select 1 from public.eat_cuisines where id=x)) then raise exception 'invalid_input_cuisine';end if;
  delete from public.eat_restaurant_cuisines where restaurant_id=p_id;
  insert into public.eat_restaurant_cuisines select p_id,x from unnest(tags) x;
 end if;
 update public.eat_restaurants set name=r.name,address=r.address,latitude=r.latitude,longitude=r.longitude,opening_hours=r.opening_hours,status=r.status,source='OSM／用家補充',updated_at=now(),version=version+1 where id=p_id;
 a:=eat_private.restaurant_json(p_id);
 foreach k in array array['name','address','latitude','longitude','opening_hours','status','cuisines'] loop if b->k is distinct from a->k then changed:=array_append(changed,k);end if;end loop;
 if 'latitude'=any(changed) or 'longitude'=any(changed) then changed:=changed||array['latitude','longitude'];end if;
 insert into eat_private.overrides values(p_id,changed) on conflict(restaurant_id) do update set fields=(select array_agg(distinct x) from unnest(eat_private.overrides.fields||excluded.fields) x);
 insert into public.eat_revisions(restaurant_id,actor_id,version,before_data,after_data,origin) values(p_id,u,(a->>'version')::bigint,b,a,'community');
 return a;
end $$;
create function public.eat_rate(p_id uuid,p_stars integer) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=eat_private.require_social();begin
 perform eat_private.limit_write('rating',60);if p_stars not between 1 and 5 or p_stars is null then raise exception 'invalid_input_rating';end if;
 if not exists(select 1 from public.eat_restaurants where id=p_id and enabled) then raise exception 'not_found';end if;
 insert into public.eat_ratings values(p_id,u,p_stars,false,now()) on conflict(restaurant_id,user_id) do update set stars=excluded.stars,updated_at=now();
 return eat_private.restaurant_json(p_id);
end $$;
create function public.eat_comment(p_id uuid,p_body text,p_request_id uuid) returns uuid language plpgsql security definer set search_path='' as $$
declare u uuid:=eat_private.require_social();old public.eat_comments%rowtype;rid uuid;begin
 select * into old from public.eat_comments where user_id=u and request_id=p_request_id;
 if found then if old.restaurant_id<>p_id or old.body<>btrim(p_body) then raise exception 'invalid_input_request';end if;return old.id;end if;
 perform eat_private.limit_write('comment',10);
 if not exists(select 1 from public.eat_restaurants where id=p_id and enabled) then raise exception 'not_found';end if;
 insert into public.eat_comments(restaurant_id,user_id,body,request_id) values(p_id,u,btrim(p_body),p_request_id) returning id into rid;return rid;
end $$;
create function public.eat_delete_comment(p_comment_id uuid) returns boolean language plpgsql security definer set search_path='' as $$
declare u uuid:=eat_private.require_social();begin perform eat_private.limit_write('comment',10);delete from public.eat_comments where id=p_comment_id and user_id=u;if not found then raise exception 'forbidden';end if;return true;end $$;
create function public.eat_set_nickname(p_nickname text) returns boolean language plpgsql security definer set search_path='' as $$
declare u uuid:=eat_private.require_social();begin perform eat_private.limit_write('profile',10);update public.eat_profiles set nickname=btrim(p_nickname) where id=u;return true;end $$;
create function public.eat_revisions(p_id uuid,p_before_version bigint default null) returns jsonb language sql stable security definer set search_path='' as $$
select coalesce(jsonb_agg(x.data order by x.version desc),'[]'::jsonb) from (select r.version,jsonb_build_object('id',r.id,'version',r.version,'nickname',coalesce(p.nickname,case when r.origin='osm_import' then 'OpenStreetMap' else '已刪除帳戶' end),'created_at',r.created_at,'before',r.before_data-'average_rating'-'rating_count','after',r.after_data-'average_rating'-'rating_count') data from public.eat_revisions r join public.eat_restaurants e on e.id=r.restaurant_id and e.enabled left join public.eat_profiles p on p.id=r.actor_id where r.restaurant_id=p_id and (p_before_version is null or r.version<p_before_version) order by r.version desc limit 10) x$$;
create function public.eat_restore_revision(p_id uuid,p_revision_id bigint,p_expected_version bigint) returns jsonb language plpgsql security definer set search_path='' as $$
declare b jsonb;patch jsonb;begin
 perform eat_private.require_social();select before_data into b from public.eat_revisions where id=p_revision_id and restaurant_id=p_id;
 if b is null then raise exception 'not_found';end if;
 patch:=jsonb_build_object('name',b->'name','address',b->'address','latitude',b->'latitude','longitude',b->'longitude','opening_hours',b->'opening_hours','status',b->'status','cuisine_ids',coalesce((select jsonb_agg(x->>'id') from jsonb_array_elements(b->'cuisines') x),'[]'::jsonb));
 return public.eat_edit_restaurant(p_id,p_expected_version,patch);
end $$;
create function public.eat_report(p_restaurant_id uuid default null,p_comment_id uuid default null,p_reason text default '') returns uuid language plpgsql security definer set search_path='' as $$
declare u uuid:=eat_private.require_social();rid uuid;begin
 perform eat_private.limit_write('report',10);
 if (p_restaurant_id is null)=(p_comment_id is null) then raise exception 'invalid_input_target';end if;
 insert into public.eat_reports(reporter_id,restaurant_id,comment_id,reason) values(u,p_restaurant_id,p_comment_id,btrim(p_reason)) returning id into rid;return rid;
end $$;
create function public.eat_list_reports(p_before timestamptz default null) returns jsonb language plpgsql security definer set search_path='' as $$
begin perform eat_private.require_admin();return coalesce((select jsonb_agg(x.data order by x.created_at desc) from (select r.created_at,jsonb_build_object('id',r.id,'restaurant_id',r.restaurant_id,'comment_id',r.comment_id,'reason',r.reason,'created_at',r.created_at) data from public.eat_reports r where r.resolved_at is null and (p_before is null or r.created_at<p_before) order by r.created_at desc,r.id desc limit 20) x),'[]'::jsonb);end $$;
create function public.eat_resolve_report(p_id uuid,p_action text) returns boolean language plpgsql security definer set search_path='' as $$
declare u uuid:=eat_private.require_admin();r public.eat_reports%rowtype;begin
 select * into r from public.eat_reports where id=p_id and resolved_at is null for update;if not found then raise exception 'not_found';end if;
 if p_action='hide_comment' and r.comment_id is not null then update public.eat_comments set hidden=true where id=r.comment_id;
 elsif p_action='disable_restaurant' and r.restaurant_id is not null then update public.eat_restaurants set enabled=false,version=version+1,updated_at=now() where id=r.restaurant_id;
 elsif p_action<>'dismiss' then raise exception 'invalid_input_action';end if;
 update public.eat_reports set resolved_at=now(),resolved_by=u,resolution=p_action where id=p_id;return true;
end $$;
create or replace function public.eat_my_profile() returns text language sql stable security definer set search_path='' as $$select nickname from public.eat_profiles where id=auth.uid()$$;
-- Every public and private table has RLS; app writes only through checked RPCs.
alter table public.eat_profiles enable row level security;alter table public.eat_cuisines enable row level security;alter table public.eat_restaurants enable row level security;alter table public.eat_restaurant_cuisines enable row level security;alter table public.eat_ratings enable row level security;alter table public.eat_comments enable row level security;alter table public.eat_revisions enable row level security;alter table public.eat_reports enable row level security;
alter table eat_private.admins enable row level security;alter table eat_private.rate_buckets enable row level security;alter table eat_private.catalog_state enable row level security;alter table eat_private.overrides enable row level security;alter table eat_private.snapshots enable row level security;alter table eat_private.snapshot_candidates enable row level security;alter table eat_private.draws enable row level security;
create policy eat_profile_own on public.eat_profiles for select to authenticated using(id=auth.uid());
create policy eat_cuisine_read on public.eat_cuisines for select to anon,authenticated using(true);
create policy eat_restaurant_read on public.eat_restaurants for select to anon,authenticated using(enabled);
create policy eat_tags_read on public.eat_restaurant_cuisines for select to anon,authenticated using(exists(select 1 from public.eat_restaurants r where r.id=restaurant_id and r.enabled));
create policy eat_rating_own on public.eat_ratings for select to authenticated using(user_id=auth.uid());
create policy eat_comment_read on public.eat_comments for select to anon,authenticated using(not hidden and exists(select 1 from public.eat_restaurants r where r.id=restaurant_id and r.enabled));
revoke all on public.eat_profiles,public.eat_cuisines,public.eat_restaurants,public.eat_restaurant_cuisines,public.eat_ratings,public.eat_comments,public.eat_revisions,public.eat_reports from anon,authenticated;
grant select on public.eat_cuisines,public.eat_restaurants,public.eat_restaurant_cuisines to anon,authenticated;
grant select(id,nickname) on public.eat_profiles to authenticated;
grant select on public.eat_ratings to authenticated;
grant select(id,restaurant_id,body,created_at) on public.eat_comments to anon,authenticated;
do $$declare f record;begin
 for f in select p.oid::regprocedure signature from pg_proc p join pg_namespace n on n.oid=p.pronamespace where n.nspname='eat_private' or (n.nspname='public' and p.proname like 'eat_%') loop execute format('revoke all on function %s from public,anon,authenticated',f.signature);end loop;
end $$;
grant execute on function public.eat_list_cuisines(),public.eat_is_admin(),public.eat_create_snapshot(double precision,double precision,integer,text[]),public.eat_prepare_draw(uuid,text,uuid,text,text,uuid),public.eat_restaurant_detail(uuid,timestamptz,uuid),public.eat_revisions(uuid,bigint) to anon,authenticated;
grant execute on function public.eat_my_profile() to authenticated;
grant execute on function public.eat_edit_restaurant(uuid,bigint,jsonb),public.eat_rate(uuid,integer),public.eat_comment(uuid,text,uuid),public.eat_delete_comment(uuid),public.eat_set_nickname(text),public.eat_restore_revision(uuid,bigint,bigint),public.eat_report(uuid,uuid,text),public.eat_list_reports(timestamptz),public.eat_resolve_report(uuid,text) to authenticated;
commit;
