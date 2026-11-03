'use strict';
(() => {
 const choice=document.querySelector('#motion-choice'),button=document.querySelector('#motion-play');
 const canvas=document.querySelector('#motion-canvas'),ctx=canvas.getContext('2d');
 const status=document.querySelector('#motion-status'),description=document.querySelector('#motion-description');
 const preference=matchMedia('(prefers-reduced-motion: reduce)');
 let clips=[],clip=null,picture=null,frame=0,elapsed=0,last=0,playing=!preference.matches,ticket=0;
 function label(){button.textContent=playing?'暫停動畫':'播放動畫';button.setAttribute('aria-pressed',String(playing))}
 function draw(){
  if(!picture)return;
  const x=Math.floor((frame%4)*picture.naturalWidth/4),right=Math.floor((frame%4+1)*picture.naturalWidth/4);
  const y=Math.floor(Math.floor(frame/4)*picture.naturalHeight/2),bottom=Math.floor((Math.floor(frame/4)+1)*picture.naturalHeight/2);
  ctx.clearRect(0,0,640,640);ctx.drawImage(picture,x,y,right-x,bottom-y,0,0,640,640);
 }
 function select(){
  const token=++ticket;clip=clips.find(c=>c.id===choice.value);picture=null;frame=0;elapsed=0;
  ctx.clearRect(0,0,640,640);button.disabled=true;status.textContent='正在載入動畫…';
  description.textContent=clip.description+(clip.cuisines.length?' 對應：'+clip.cuisines.join('、')+'。':'');
  canvas.setAttribute('aria-label',clip.title+'動畫示例');
  const img=new Image();img.onload=()=>{if(token!==ticket)return;picture=img;draw();button.disabled=false;status.textContent='已載入：'+clip.title+'。可暫停查看；本示例不進行餐廳抽選。';label()};
  img.onerror=()=>{if(token!==ticket)return;status.textContent='這款素材未能載入，可改選其他造型或查看下方原圖。'};
  img.src=clip.src;
 }
 button.onclick=()=>{playing=!playing;label()};choice.onchange=select;
 preference.addEventListener('change',e=>{if(e.matches){playing=false;label()}});
 function tick(now){const delta=Math.min(now-last,100);last=now;if(playing&&picture&&!document.hidden){elapsed+=delta;while(elapsed>=clip.times[frame]){elapsed-=clip.times[frame];frame=(frame+1)%8;draw()}}requestAnimationFrame(tick)}
 button.disabled=true;label();requestAnimationFrame(tick);
 fetch('assets/motion-clips.json').then(r=>{if(!r.ok)throw Error('load');return r.json()}).then(data=>{
  clips=data;for(const c of clips)choice.add(new Option(c.title,c.id));choice.value='ramen';select();
 }).catch(()=>{status.textContent='動畫清單未能載入，下方仍可查看原圖及菜式對照。'});
})();
