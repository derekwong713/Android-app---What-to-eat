begin;
create table eat_private.osm_sources (
 object_type text not null check(object_type in ('node','way','relation')),object_id bigint not null,
 restaurant_id uuid not null unique references public.eat_restaurants(id),source_version text not null,
 raw_tags jsonb not null,observed_at timestamptz not null,primary key(object_type,object_id)
);
create table eat_private.osm_versions (
 object_type text not null,object_id bigint not null,source_version text not null,raw_data jsonb not null,
 imported_at timestamptz not null default now(),primary key(object_type,object_id,source_version)
);
alter table eat_private.osm_sources enable row level security;alter table eat_private.osm_versions enable row level security;
create function public.eat_import_osm(p_records jsonb) returns jsonb language plpgsql security definer set search_path='' as $$
declare item jsonb;old eat_private.osm_sources%rowtype;r public.eat_restaurants%rowtype;rid uuid;b jsonb;a jsonb;fields text[];c jsonb;added integer:=0;updated integer:=0;skipped integer:=0;oldtags jsonb;
begin
 if auth.role()<>'service_role' then raise exception 'forbidden';end if;
 -- Serialize operator batches so overlapping imports cannot create orphan duplicate restaurants.
 perform pg_advisory_xact_lock(20261027,1);
 if jsonb_typeof(p_records)<>'array' or jsonb_array_length(p_records)>500 then raise exception 'invalid_input_batch';end if;
 for item in select value from jsonb_array_elements(p_records) loop
  if item->>'object_type' not in ('node','way','relation') or (item->>'object_id')::bigint<=0 or char_length(item->>'source_version') not between 1 and 100 then raise exception 'invalid_input_source';end if;
  if exists(select 1 from eat_private.osm_versions where object_type=item->>'object_type' and object_id=(item->>'object_id')::bigint and source_version=item->>'source_version') then skipped:=skipped+1;continue;end if;
  select * into old from eat_private.osm_sources where object_type=item->>'object_type' and object_id=(item->>'object_id')::bigint for update;
  if found and old.observed_at>(item->>'observed_at')::timestamptz then skipped:=skipped+1;continue;end if;
  if old.restaurant_id is null then
   rid:=gen_random_uuid();
   insert into public.eat_restaurants(id,name,address,latitude,longitude,opening_hours,status,source) values(rid,item->>'name',coalesce(item->>'address',''),(item->>'latitude')::double precision,(item->>'longitude')::double precision,item->>'opening_hours',coalesce(item->>'status','unknown'),'OpenStreetMap');
   fields:='{}';added:=added+1;
  else
   rid:=old.restaurant_id;select * into r from public.eat_restaurants where id=rid for update;b:=eat_private.restaurant_json(rid);
   select coalesce(o.fields,'{}') into fields from eat_private.overrides o where o.restaurant_id=rid;fields:=coalesce(fields,'{}');
   update public.eat_restaurants set
    name=case when 'name'=any(fields) then name else item->>'name' end,
    address=case when 'address'=any(fields) then address else coalesce(item->>'address','') end,
    latitude=case when 'latitude'=any(fields) then latitude else (item->>'latitude')::double precision end,
    longitude=case when 'longitude'=any(fields) then longitude else (item->>'longitude')::double precision end,
    opening_hours=case when 'opening_hours'=any(fields) then opening_hours else item->>'opening_hours' end,
    status=case when 'status'=any(fields) then status else coalesce(item->>'status','unknown') end,
    updated_at=now(),version=version+1 where id=rid;
   updated:=updated+1;
  end if;
  if not ('cuisines'=any(fields)) then
   delete from public.eat_restaurant_cuisines where restaurant_id=rid;
   if jsonb_array_length(coalesce(item->'cuisines','[]'::jsonb))>32 then raise exception 'invalid_input_cuisine';end if;
   for c in select value from jsonb_array_elements(coalesce(item->'cuisines','[]'::jsonb)) loop
    insert into public.eat_cuisines(id,label) values(c->>'id',c->>'label') on conflict(id) do nothing;
    insert into public.eat_restaurant_cuisines values(rid,c->>'id') on conflict do nothing;
   end loop;
  end if;
  if old.restaurant_id is not null then
   a:=eat_private.restaurant_json(rid);
   -- Disabled entries stay disabled; their private provenance still updates.
   if b is not null and a is not null then insert into public.eat_revisions(restaurant_id,version,before_data,after_data,origin) values(rid,(a->>'version')::bigint,b,a,'osm_import');end if;
  end if;
  insert into eat_private.osm_sources values(item->>'object_type',(item->>'object_id')::bigint,rid,item->>'source_version',coalesce(item->'raw_tags','{}'::jsonb),(item->>'observed_at')::timestamptz)
  on conflict(object_type,object_id) do update set source_version=excluded.source_version,raw_tags=excluded.raw_tags,observed_at=excluded.observed_at;
  insert into eat_private.osm_versions(object_type,object_id,source_version,raw_data) values(item->>'object_type',(item->>'object_id')::bigint,item->>'source_version',item);
 end loop;
 return jsonb_build_object('added',added,'updated',updated,'skipped',skipped);
end $$;
revoke all on function public.eat_import_osm(jsonb) from public,anon,authenticated;
grant usage on schema eat_private to service_role;
grant execute on function public.eat_import_osm(jsonb) to service_role;
-- Operators can schedule this service-only cleanup using Supabase Cron, or call from server tooling.
create function public.eat_cleanup_expired() returns void language plpgsql security definer set search_path='' as $$
begin
 if auth.role()<>'service_role' then raise exception 'forbidden';end if;
 delete from eat_private.snapshots where expires_at<now();delete from eat_private.draws where expires_at<now();
 delete from eat_private.rate_buckets where bucket<now()-interval '2 days';
end $$;
revoke all on function public.eat_cleanup_expired() from public,anon,authenticated;
grant execute on function public.eat_cleanup_expired() to service_role;
commit;
