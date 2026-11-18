var r=document.documentElement,b=document.body,sh=document.getElementById('sheet');
function op(){b.classList.add('open')}function cl(){b.classList.remove('open');sh.style.transform=''}
document.getElementById('fab').onclick=op;
['x','c','scrim'].forEach(function(i){document.getElementById(i).onclick=cl});
document.addEventListener('keydown',function(e){if(e.key==='Escape')cl()});
var g=document.getElementById('grab'),y0=null;
function mv(e){var d=Math.max(0,e.clientY-y0);sh.style.transform='translateY('+d+'px)'}
g.onpointerdown=function(e){y0=e.clientY;sh.classList.add('drag');g.setPointerCapture(e.pointerId);g.onpointermove=mv};
g.onpointerup=function(e){g.onpointermove=null;sh.classList.remove('drag');var d=e.clientY-y0;if(d>120)cl();else sh.style.transform=''};

function clearVars(){['--surface','--sheet','--card','--on-surface','--on-var','--outline','--outline-v'].forEach(function(v){r.style.removeProperty(v)})}
document.querySelectorAll('[data-h]').forEach(function(x){x.onclick=function(){clearVars();r.style.setProperty('--h',x.dataset.h);document.querySelectorAll('[data-h],[data-mode],[data-dyn],#wheel').forEach(function(z){z.setAttribute('aria-pressed',z===x)})}});
document.querySelectorAll('[data-mode]').forEach(function(x){x.onclick=function(){var m=x.dataset.mode;document.querySelectorAll('[data-h],[data-mode],[data-dyn],#wheel').forEach(function(z){z.setAttribute('aria-pressed',z===x)});if(m==='system'){clearVars();r.removeAttribute('data-theme')}else if(m==='black'){r.setAttribute('data-theme','dark');clearVars();r.style.setProperty('--surface','#000');r.style.setProperty('--sheet','#1c1c1c');r.style.setProperty('--card','#262626');r.style.setProperty('--on-surface','#fff');r.style.setProperty('--on-var','#c2c2c2');r.style.setProperty('--outline','#5c5c5c');r.style.setProperty('--outline-v','#333');$('wheel').style.background='#000'}else if(m==='white'){r.setAttribute('data-theme','light');clearVars();r.style.setProperty('--surface','#fff');r.style.setProperty('--sheet','#f4f4f4');r.style.setProperty('--card','#fff');r.style.setProperty('--on-surface','#000');r.style.setProperty('--on-var','#555');r.style.setProperty('--outline','#999');r.style.setProperty('--outline-v','#ddd');$('wheel').style.background='#fff'}}});
var st=0,P=document.querySelectorAll('.pn'),S=document.querySelectorAll('.steps div'),nx=document.getElementById('nx'),bk=document.getElementById('c');
function go(n){st=n;P.forEach(function(p){p.hidden=+p.dataset.s!==n});S.forEach(function(s,k){s.classList.toggle('on',k<=n)});bk.textContent=n===4?'Hide':n?'Back':'Cancel';nx.textContent=n===3?'Install':n===4?'Done':'Next';nx.disabled=n===4&&!done;document.querySelector('.body').scrollTop=0;if(n===3){var o=document.querySelector('.opt:has(input:checked)'),mn=document.getElementById('mn');document.getElementById('dl').textContent=o.querySelector('b').textContent[0];if(!mn.dataset.e)mn.value=o.querySelector('b').textContent+' '+o.querySelector('.chip.sel').textContent}}
bk.onclick=function(){st===4?cl():st?go(st-1):cl()};
nx.onclick=function(){if(st<3)go(st+1);else if(st===3)start();else cl()};
document.getElementById('fab').addEventListener('click',function(){go(0)});
document.querySelectorAll('.ver').forEach(function(v){v.querySelectorAll('.chip').forEach(function(c){c.onclick=function(e){e.preventDefault();v.querySelectorAll('.chip').forEach(function(z){z.classList.remove('sel')});c.classList.add('sel');v.closest('.opt').querySelector('input').checked=true}})});
function cn(){var n=document.querySelectorAll('.tool input:checked').length;document.getElementById('cnt').textContent='Installed with the environment. '+n+' selected.'}
document.querySelectorAll('.tool input').forEach(function(i){i.onchange=cn});
document.querySelectorAll('.sa').forEach(function(b){b.onclick=function(){var l=b.closest('.grp').querySelectorAll('input'),all=[].every.call(l,function(i){return i.checked});l.forEach(function(i){i.checked=!all});cn()}});cn();
document.getElementById('cu').onchange=function(){document.getElementById('uf').hidden=!this.checked};
document.getElementById('af').onclick=function(){var d=document.createElement('div');d.className='row2';d.innerHTML='<span><b>Folder from picker</b><small>Android folder</small></span><button type="button" class="btn t">Remove</button>';d.querySelector('button').onclick=function(){d.remove()};document.getElementById('fl').appendChild(d)};
document.querySelectorAll('.tg').forEach(function(t){t.onclick=function(){var v=t.closest('.ver'),o=t.getAttribute('aria-expanded')!=='true';v.querySelectorAll('.ex').forEach(function(c){c.hidden=!o});t.setAttribute('aria-expanded',o);t.textContent=o?'Fewer versions':'All versions'}});
var done=false;
function start(q){
var o=document.querySelector('.opt:has(input:checked)'),dn=o.querySelector('b').textContent+' '+o.querySelector('.chip.sel').textContent,nm=(document.getElementById('mn').value.trim()||dn),
tl=document.querySelectorAll('.tool input:checked').length,
ST=['Download rootfs','Extract rootfs','Install '+tl+' selected tools','Configure mounts and user'],
l=document.getElementById('il'),sp=document.getElementById('sp'),k=0;
done=false;sp.className='spin';
var r=document.querySelector('input[name=ic]:checked'),ico=r.value==='default'?dn[0]:r.closest('label').querySelector('svg').outerHTML,
TI=document.querySelector('input[name=ic][value=terminal]').closest('label').querySelector('svg').outerHTML,
NI='<svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 19H5V5h7V3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2v-7h-2v7zM14 3v2h3.59l-9.83 9.83 1.41 1.41L19 6.41V10h2V3h-7z"/></svg>',
AP={vscode:['VS Code','Opens VS Code in the in-app WebView','<svg viewBox="0 0 24 24" fill="currentColor"><path d="M23.15 2.587L18.21.21a1.494 1.494 0 0 0-1.705.29l-9.46 8.63-4.12-3.128a.999.999 0 0 0-1.276.057L.327 7.261A1 1 0 0 0 .326 8.74L3.899 12 .326 15.26a1 1 0 0 0 .001 1.479L1.65 17.94a.999.999 0 0 0 1.276.057l4.12-3.128 9.46 8.63a1.492 1.492 0 0 0 1.704.29l4.942-2.377A1.5 1.5 0 0 0 24 20.06V3.939a1.5 1.5 0 0 0-.85-1.352zm-5.146 14.861L10.826 12l7.178-5.448v10.896z"/></svg>'],opencode:['OpenCode','Opens OpenCode','<svg viewBox="0 0 24 24" fill="currentColor" fill-rule="evenodd"><path d="M4 3h16a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1zm1 2v14h14V5H5zm3 6h8v6H8v-6z"/></svg>']},
acts='<div class="acts"><button type="button" class="ab f ico t2" disabled data-t="in" aria-label="Terminal" title="Terminal" data-m="Opens the terminal inside the app">'+TI+'<span class="lt">Terminal</span></button><button type="button" class="ab ico t2" disabled aria-label="Terminal in new window" title="Terminal in new window" data-m="Opens the terminal in a new window">'+NI+'<span class="lt">Terminal in new window</span></button><button type="button" class="ab ico t2" disabled data-fs="1" aria-label="File system" title="File system"><svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z"/></svg><span class="lt">File system</span></button>';
document.querySelectorAll('.tool input[data-app]:checked').forEach(function(c){var a=AP[c.dataset.app];acts+='<button type="button" class="ab ico" disabled aria-label="'+a[0]+'" title="'+a[0]+'" data-m="'+a[1]+'">'+a[2]+'</button>'});acts+='</div>';
l.innerHTML=ST.map(function(s){return '<li><i></i>'+s+'</li>'}).join('');
document.getElementById('it').textContent='Creating environment';
document.getElementById('is').textContent='';
if(!q)go(4);
var env=document.createElement('article');env.className='env';var eid='e'+(++en),un=document.querySelector('#uf input');env.id=eid;ENV[eid]={name:nm,host:nm.toLowerCase().replace(/[^a-z0-9]+/g,'-').replace(/^-|-$/g,'')||'machine',user:(document.getElementById('cu').checked&&un.value.trim())||'user',ss:[],cur:0,pv:-1,adb:document.getElementById('adbx').checked};
env.innerHTML='<div class="eh"><div class="mark">'+ico+'</div><div><b>'+nm+'</b><small>Installing…</small></div><button type="button" class="mn" aria-label="Settings"><svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M12 8a2 2 0 1 0 0-4 2 2 0 0 0 0 4zm0 2a2 2 0 1 0 0 4 2 2 0 0 0 0-4zm0 6a2 2 0 1 0 0 4 2 2 0 0 0 0-4z"/></svg></button></div><div class="lp"><i></i></div>'+acts+'<div class="xp"><div><div class="pvs"></div><div class="pvb" hidden></div></div></div>';
document.querySelector('.empty').hidden=true;
document.getElementById('envs').prepend(env);
env.querySelector('.mn').onclick=function(e){e.stopPropagation();if(reorder)return;openDetail(eid)};
env.querySelector('.eh').onclick=function(){if(env.dataset.sup==='1'){env.dataset.sup='';return}if(reorder)return;openOv(eid)};
attachEnvDrag(env);
if(q)k=4;
(function t(){
if(k>0)l.children[k-1].className='ok';
if(k===4){done=true;sp.className='spin done';document.getElementById('it').textContent='Environment ready';document.getElementById('is').textContent=nm;env.querySelector('small').textContent='RAM — · Space —';env.querySelector('.lp').remove();env.querySelectorAll('.ab').forEach(function(b){b.disabled=false;b.onclick=function(){if(b.dataset.fs==='1')openFs();else if(b.dataset.t==='in')openT(eid);else toast(b.dataset.m)}});if(st===4)nx.disabled=false;return}
l.children[k].className='on';document.getElementById('is').textContent='Step '+(k+1)+' of 4 · '+ST[k];k++;setTimeout(t,1500)})();
}
var T={s:[],cur:0,name:'',host:'',user:'user'};
function selS(k){T.cur=k;T.s.forEach(function(d,j){d.hidden=j!==k});[].forEach.call(document.getElementById('tabs').children,function(b,j){b.setAttribute('aria-selected',j===k)})}
function addS(){var d=document.createElement('div'),k=T.s.length,b=document.createElement('button');d.className='ts';d.hidden=true;d.innerHTML='<div class="mut">Mockup: no shell attached.</div>';document.getElementById('tout').appendChild(d);T.s.push(d);b.type='button';b.className='tab';b.setAttribute('role','tab');b.textContent='Session '+(k+1);b.onclick=function(){selS(k)};document.getElementById('tabs').appendChild(b);selS(k)}
function openT(nm){var un=document.querySelector('#uf input');if(T.name!==nm){T.name=nm;T.s=[];document.getElementById('tabs').innerHTML='';document.getElementById('tout').innerHTML='';addS()}T.host=nm.toLowerCase().replace(/[^a-z0-9]+/g,'-').replace(/^-|-$/g,'')||'machine';T.user=(document.getElementById('cu').checked&&un.value.trim())||'user';document.getElementById('tp').textContent=T.user+'@'+T.host+':~$';document.getElementById('term').classList.add('on')}
document.getElementById('tback').onclick=function(){document.getElementById('term').classList.remove('on')};
document.getElementById('tnew').onclick=addS;
document.getElementById('ti').onkeydown=function(e){if(e.key==='Enter'&&this.value.trim()){var l=document.createElement('div'),p=document.createElement('span'),o=document.getElementById('tout');p.className='pr';p.textContent=document.getElementById('tp').textContent+' ';l.appendChild(p);l.appendChild(document.createTextNode(this.value));T.s[T.cur].appendChild(l);this.value='';o.scrollTop=o.scrollHeight}};
var GRIP='<svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M11 18c0 1.1-.9 2-2 2s-2-.9-2-2 .9-2 2-2 2 .9 2 2zm-2-8c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0-6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm6 4c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"/></svg>',
XI='<svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>',
PEN='<svg viewBox="0 0 24 24" fill="currentColor"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>',
KS=[{id:'esc',l:'Esc',k:'Esc'},{id:'tab',l:'Tab',k:'Tab'},{id:'ctrl',l:'Ctrl',k:'Ctrl',t:'m'},{id:'alt',l:'Alt',k:'Alt',t:'m'},{id:'up',l:'↑',k:'Up'},{id:'down',l:'↓',k:'Down'},{id:'left',l:'←',k:'Left'},{id:'right',l:'→',k:'Right'},{id:'sl',l:'/',k:'/',t:'x'},{id:'mi',l:'-',k:'-',t:'x'},{id:'pp',l:'|',k:'|',t:'x'},{id:'tl',l:'~',k:'~',t:'x'}].map(function(x){x.s=true;return x}),KD=JSON.stringify(KS),kn=0;
function bs(id,on){document.getElementById(id).classList.toggle('on',on)}
function rk(){var b=document.getElementById('keys');b.innerHTML='';KS.filter(function(x){return x.s}).forEach(function(x){var e=document.createElement('button');e.type='button';e.className='kb';e.textContent=x.l;if(x.t==='m')e.setAttribute('aria-pressed','false');e.onclick=function(){var ti=document.getElementById('ti');if(x.t==='m')e.setAttribute('aria-pressed',e.getAttribute('aria-pressed')!=='true');else if(x.t==='x'){ti.value+=x.k;ti.focus()}else toast('Sends '+x.k)};b.appendChild(e)});var c=document.createElement('button');c.type='button';c.className='kb';c.setAttribute('aria-label','Edit shortcuts');c.title='Edit shortcuts';c.innerHTML=PEN;c.onclick=openK;b.appendChild(c)}
function rl(){var kl=document.getElementById('kl');kl.innerHTML='';KS.forEach(function(x){var li=document.createElement('li');li.className='kr';li.dataset.id=x.id;li.innerHTML='<button type="button" class="kh">'+GRIP+'</button><span class="kn"></span><label class="swl"><input type="checkbox"><span class="sw"></span></label>'+(x.c?'<button type="button" class="kx" aria-label="Delete">'+XI+'</button>':'');li.querySelector('.kn').textContent=x.l+(x.c?'  →  '+x.k:'');var h=li.querySelector('.kh'),cb=li.querySelector('input');h.setAttribute('aria-label','Reorder '+x.l+'. Drag, or use the up and down arrow keys');cb.checked=x.s;cb.onchange=function(){x.s=cb.checked;rk()};if(x.c)li.querySelector('.kx').onclick=function(){KS=KS.filter(function(y){return y!==x});rl();rk()};
h.onpointerdown=function(e){e.preventDefault();h.setPointerCapture(e.pointerId);li.classList.add('drg')};
h.onpointermove=function(e){if(!li.classList.contains('drg'))return;var o=[].filter.call(kl.children,function(r){return r!==li}).filter(function(r){var q=r.getBoundingClientRect();return e.clientY<q.top+q.height/2})[0];if(o)kl.insertBefore(li,o);else kl.appendChild(li)};
h.onpointerup=function(){li.classList.remove('drg');KS=[].map.call(kl.children,function(r){return KS.filter(function(y){return y.id===r.dataset.id})[0]});rk()};
h.onkeydown=function(e){var a=KS.indexOf(x),j=e.key==='ArrowUp'?a-1:e.key==='ArrowDown'?a+1:-1;if(j<0||j>=KS.length)return;e.preventDefault();KS.splice(j,0,KS.splice(a,1)[0]);rl();rk();kl.querySelector('[data-id="'+x.id+'"] .kh').focus()};
kl.appendChild(li)})}
function openK(){rl();bs('kmod',true)}
document.querySelectorAll('.bsx').forEach(function(m){m.querySelectorAll('[data-close]').forEach(function(c){c.onclick=function(){bs(m.id,false)}})});
document.getElementById('kreset').onclick=function(){KS=JSON.parse(KD);rl();rk()};
document.getElementById('kadd').onclick=function(){var a=document.getElementById('ka1'),b=document.getElementById('ka2');if(!a.value.trim()||!b.value)return;KS.push({id:'c'+(++kn),l:a.value.trim(),k:b.value,t:'x',s:true,c:true});a.value='';b.value='';rl();rk()};
document.getElementById('pf').onclick=function(){bs('pmod',true)};
document.getElementById('plb').onchange=function(){document.body.classList.toggle('lbl',this.checked)};
document.getElementById('mn').oninput=function(){this.dataset.e=this.value?'1':''};
var IC={home:'M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z',star:'M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z',favorite:'M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z',cloud:'M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96z',folder:'M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z',build:'M22.7 19l-9.1-9.1c.9-2.3.4-5-1.5-6.9-2-2-5-2.4-7.4-1.3L9 6 6 9 1.6 4.7C.4 7.1.9 10.1 2.9 12.1c1.9 1.9 4.6 2.4 6.9 1.5l9.1 9.1c.4.4 1 .4 1.4 0l2.3-2.3c.5-.4.5-1.1.1-1.4z',lock:'M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z',bolt:'M7 2v11h3v9l7-12h-4l4-8z',school:'M5 13.18v4L12 21l7-3.82v-4L12 17l-7-3.82zM12 3L1 9l11 6 9-4.91V17h2V9L12 3z',work:'M20 6h-4V4c0-1.11-.89-2-2-2h-4c-1.11 0-2 .89-2 2v2H4c-1.11 0-1.99.89-1.99 2L2 19c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V8c0-1.11-.89-2-2-2zm-6 0h-4V4h4v2z',storage:'M2 20h20v-4H2v4zm2-3h2v2H4v-2zM2 4v4h20V4H2zm4 3H4V5h2v2zm-4 7h20v-4H2v4zm2-3h2v2H4v-2z',memory:'M15 9H9v6h6V9zm-2 4h-2v-2h2v2zm8-2V9h-2V7c0-1.1-.9-2-2-2h-2V3h-2v2h-2V3H9v2H7c-1.1 0-2 .9-2 2v2H3v2h2v2H3v2h2v2c0 1.1.9 2 2 2h2v2h2v-2h2v2h2v-2h2c1.1 0 2-.9 2-2v-2h2v-2h-2v-2h2zm-4 6H7V7h10v10z',public:'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z',science:'M19.8 18.4L14 10.67V6.5l1.35-1.69c.26-.33.03-.81-.39-.81H9.04c-.42 0-.65.48-.39.81L10 6.5v4.17L4.2 18.4c-.49.66-.02 1.6.8 1.6h14c.82 0 1.29-.94.8-1.6z',extension:'M20.5 11H19V7c0-1.1-.9-2-2-2h-4V3.5C13 2.12 11.88 1 10.5 1S8 2.12 8 3.5V5H4c-1.1 0-1.99.9-1.99 2v3.8H3.5c1.49 0 2.7 1.21 2.7 2.7s-1.21 2.7-2.7 2.7H2V20c0 1.1.9 2 2 2h3.8v-1.5c0-1.49 1.21-2.7 2.7-2.7 1.49 0 2.7 1.21 2.7 2.7V22H17c1.1 0 2-.9 2-2v-4h1.5c1.38 0 2.5-1.12 2.5-2.5S21.88 11 20.5 11z',laptop:'M20 18c1.1 0 1.99-.9 1.99-2L22 6c0-1.1-.9-2-2-2H4c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2H0v2h24v-2h-4zM4 6h16v10H4V6z',music_note:'M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z',bookmark:'M17 3H7c-1.1 0-1.99.9-1.99 2L5 21l7-3 7 3V5c0-1.1-.9-2-2-2z',dashboard:'M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z',shield:'M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z'},pg=document.getElementById('pg');
Object.keys(IC).forEach(function(n){var b=document.createElement('button');b.type='button';b.className='pi';b.title=n.replace('_',' ');b.setAttribute('aria-label',n.replace('_',' '));b.dataset.n=n;b.innerHTML='<svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="'+IC[n]+'"/></svg>';b.onclick=function(){var c=document.getElementById('cust');c.hidden=false;c.querySelector('.ib').innerHTML=b.innerHTML;c.querySelector('input').checked=true;c.querySelector('small').textContent=n.replace('_',' ');bs('imod',false)};pg.appendChild(b)});
document.getElementById('icmore').onclick=function(){bs('imod',true)};
document.getElementById('isr').oninput=function(){var q=this.value.toLowerCase().replace(' ','_');[].forEach.call(pg.children,function(b){b.hidden=b.dataset.n.indexOf(q)<0})};
rk();
var CE=null,en=0,ENV={};
function $(i){return document.getElementById(i)}
function ln(x){var d=document.createElement('div');if(x.m){d.className='mut';d.textContent=x.m}else{var p=document.createElement('span');p.className='pr';p.textContent=x.p+' ';d.appendChild(p);d.appendChild(document.createTextNode(x.c))}return d}
function chip(t,on){var b=document.createElement('button');b.type='button';b.className='pc';b.textContent=t;b.setAttribute('aria-pressed',on);return b}
function rend(){var E=ENV[CE],t=$('tabs'),o=$('tout');t.innerHTML='';E.ss.forEach(function(s,k){var b=document.createElement('button');b.type='button';b.className='tab';b.setAttribute('role','tab');b.setAttribute('aria-selected',k===E.cur);b.textContent='Session '+(k+1);b.onclick=function(){E.cur=k;rend()};var w=document.createElement('div');w.className='tabw';w.appendChild(b);if(k===E.cur){var x=document.createElement('button');x.type='button';x.className='tcl';x.setAttribute('aria-label','Close Session '+(k+1));x.title='Close session';x.innerHTML=XS;x.onclick=function(){closeS(k)};w.appendChild(x)}t.appendChild(w)});o.innerHTML='';E.ss[E.cur].l.forEach(function(x){o.appendChild(ln(x))});o.scrollTop=o.scrollHeight}
function upd(id){var E=ENV[id],c=$(id);if(!c)return;var xp=c.querySelector('.xp'),pvs=c.querySelector('.pvs'),pb=c.querySelector('.pvb');xp.classList.toggle('on',E.ss.length>0);pvs.innerHTML='';E.ss.forEach(function(s,k){var b=chip('Session '+(k+1),E.pv===k);b.onclick=function(){E.pv=k;upd(id)};pvs.appendChild(b)});var nb=chip('None',E.pv<0);nb.onclick=function(){E.pv=-1;upd(id)};pvs.appendChild(nb);pb.hidden=E.pv<0;pb.innerHTML='';if(E.pv>=0){E.ss[E.pv].l.slice(-5).forEach(function(x){pb.appendChild(ln(x))})}pb.onclick=function(){if(E.pv>=0){E.cur=E.pv;openT(id)}}}
function addS(){var E=ENV[CE];E.ss.push({l:[{m:'Mockup: no shell attached.'}]});E.cur=E.ss.length-1;if(E.ss.length===1)E.pv=0;rend();upd(CE)}
function openT(id){CE=id;var E=ENV[id];if(E.ss.length)rend();else addS();$('term').setAttribute('aria-label',E.name+' terminal');$('tp').textContent=E.user+'@'+E.host+':~$';$('term').classList.add('on')}
$('ti').onkeydown=function(e){if(e.key==='Enter'&&this.value.trim()){var E=ENV[CE],x={p:$('tp').textContent,c:this.value};E.ss[E.cur].l.push(x);$('tout').appendChild(ln(x));this.value='';$('tout').scrollTop=$('tout').scrollHeight;upd(CE)}};
var XS='<svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>';
function closeS(k){var E=ENV[CE];E.ss.splice(k,1);if(E.pv===k)E.pv=E.ss.length?0:-1;else if(E.pv>k)E.pv--;if(k<E.cur)E.cur--;if(E.cur>=E.ss.length)E.cur=E.ss.length-1;if(E.cur<0)E.cur=0;upd(CE);if(!E.ss.length){$('term').classList.remove('on');return}rend()}
var ADB={ok:false},aw=0;
function adbh(){var t=ADB.ok?'ADB is connected.':'ADB isn’t set up yet.',c=ADB.ok?'':'#d93025';var w=$('adbh');if(w){w.textContent=t;w.style.color=c}var d=$('edadbh');if(d){d.textContent=t;d.style.color=c}var s=$('adbset');if(s)s.hidden=ADB.ok;var e=$('edadbset');if(e)e.hidden=ADB.ok}
function adbu(){$('adbl').textContent=ADB.ok?'ADB connected':'ADB setup';adbh()}
function ago(n){aw=n;[].forEach.call(document.querySelectorAll('.ap'),function(p){p.hidden=+p.dataset.a!==n});[].forEach.call($('ast').children,function(s,k){s.classList.toggle('on',k<=Math.min(n,2))});$('aback').textContent=n===0||n===3?'Cancel':'Back';$('aback').hidden=n===3;$('anext').textContent=['Next','Pair','Connect','Done'][n];$('anext').disabled=false}
function aopen(){ago(ADB.ok?3:0);bs('amod',true)}
$('adbb').onclick=aopen;$('adbset').onclick=aopen;
$('aback').onclick=function(){aw===0||aw===3?bs('amod',false):ago(aw-1)};
$('adev').onclick=function(){toast('Opens Developer options')};
$('adis').onclick=function(){ADB.ok=false;adbu();ago(0)};
$('anext').onclick=function(){var b=this;if(aw===0)ago(1);else if(aw===3)bs('amod',false);else{var f=aw===1?[$('acode'),$('apair')]:[$('aconn')];if(f.some(function(x){return !x.value.trim()})){toast('Fill in all fields');return}b.disabled=true;b.textContent=aw===1?'Pairing…':'Connecting…';setTimeout(function(){if(aw===1)ago(2);else{ADB.ok=true;adbu();ago(3)}},1300)}};
adbu();
function toast(t){var s=document.getElementById('snk');s.textContent=t;s.classList.add('show');clearTimeout(s._t);s._t=setTimeout(function(){s.classList.remove('show')},2200)}
var m=['system','light','dark'],i=0;
document.querySelectorAll('[data-tm]').forEach(function(x){x.onclick=function(){document.querySelectorAll('[data-tm]').forEach(function(z){z.setAttribute('aria-pressed',z===x)});if(x.dataset.tm==='system')r.removeAttribute('data-theme');else r.setAttribute('data-theme',x.dataset.tm)}});
document.querySelectorAll('.tool input[data-app]').forEach(function(c){c.checked=true});start(true);document.querySelectorAll('.tool input[data-app]').forEach(function(c){c.checked=false});cn();
var NT={ss:[],cur:0};
function openNT(){NT.ss=[];$('tabs').innerHTML='';$('tout').innerHTML='';var d=document.createElement('div');d.className='ts';d.innerHTML='<div class="mut">Native terminal (Android shell). No proot.</div>';$('tout').appendChild(d);NT.ss.push(d);var b=document.createElement('button');b.type='button';b.className='tab';b.setAttribute('role','tab');b.textContent='shell';b.setAttribute('aria-selected','true');$('tabs').appendChild(b);$('term').setAttribute('aria-label','Native terminal');$('tp').textContent='localhost:~$';$('term').classList.add('on')}
$('nativeterm').onclick=openNT;
var DETAIL=null;
function openDetail(id){DETAIL=id;var E=ENV[id];$('edname').textContent=E.name;$('edsub').textContent='Ready';$('edmn').value=E.name;$('edmn').dataset.e='1';$('eddl').textContent=(E.name||'U')[0];adbh();$('envd').classList.add('on')}
function showEtab(p){document.querySelectorAll('.epan').forEach(function(x){x.hidden=x.dataset.p!==p});document.querySelectorAll('.etab').forEach(function(x){x.setAttribute('aria-selected',x.dataset.p===p)});if(p==='fl')renderFiles('/home/user')}
function closeDetail(){$('envd').classList.remove('on');DETAIL=null}
$('edback').onclick=closeDetail;
document.querySelectorAll('.etab').forEach(function(t){t.onclick=function(){showEtab(t.dataset.p)}});
var FILES={'/':[{n:'home',t:'folder'},{n:'usr',t:'folder'},{n:'etc',t:'folder'},{n:'var',t:'folder'},{n:'opt',t:'folder'},{n:'tmp',t:'folder'}],'/home':[{n:'user',t:'folder'}],'/home/user':[{n:'projects',t:'folder'},{n:'Downloads',t:'folder',m:true},{n:'.bashrc',t:'file',s:'3.2 KB'},{n:'.gitconfig',t:'file',s:'284 B'},{n:'notes.txt',t:'file',s:'1.1 KB'}],'/home/user/projects':[{n:'webapp',t:'folder'},{n:'api-server',t:'folder'},{n:'README.md',t:'file',s:'892 B'}],'/home/user/Downloads':[{n:'document.pdf',t:'file',s:'2.4 MB'},{n:'image.png',t:'file',s:'1.8 MB'}],'/usr':[{n:'bin',t:'folder'},{n:'lib',t:'folder'},{n:'share',t:'folder'},{n:'local',t:'folder'}],'/etc':[{n:'apt',t:'folder'},{n:'hosts',t:'file',s:'220 B'},{n:'passwd',t:'file',s:'1.9 KB'}],'/var':[{n:'log',t:'folder'},{n:'tmp',t:'folder'}],'/opt':[{n:'code-server',t:'folder'}],'/tmp':[{n:'tmpfile',t:'file',s:'12 B'}]};
var FP='/home/user';
function crumbHtml(path){var parts=path.split('/').filter(Boolean);var h='<button type="button" class="fpc" data-p="/">/</button>';var cur='/';parts.forEach(function(p){cur+='/'+p;h+='<button type="button" class="fpc'+(cur===path?' cur':'')+'" data-p="'+cur+'">/'+p+'</button>'});return h}
function renderFiles(path){FP=path;var fp=$('fpath'),fl=$('flist');fp.innerHTML=crumbHtml(path);fp.querySelectorAll('[data-p]').forEach(function(s){s.onclick=function(){renderFiles(s.dataset.p)}});fl.innerHTML='';var items=(path==='/'?[]:[{n:'..',t:'up'}]).concat(FILES[path]||[]);items.forEach(function(f){var b=document.createElement('button');b.className='fitem';var ic=f.t==='folder'?'<svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z"/></svg>':f.t==='up'?'<svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>':'<svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/></svg>';b.innerHTML=ic+'<div><b>'+f.n+'</b>'+(f.m?'<small>Mounted</small>':'')+'</div>'+(f.s?'<span class="fs">'+f.s+'</span>':'');b.onclick=function(){if(f.t==='folder'){var np=(path==='/'?'':path)+'/'+f.n;if(!FILES[np])FILES[np]=[];renderFiles(np)}else if(f.t==='up'){var pp=path.split('/').slice(0,-1).join('/')||'/';renderFiles(pp)}else toast('Mockup: no file viewer')};fl.appendChild(b)})}
document.querySelectorAll('.bkdel').forEach(function(b){b.onclick=function(){b.closest('.bkitem').remove();toast('Backup deleted')}});
$('edaf').onclick=function(){var d=document.createElement('div');d.className='row2';d.innerHTML='<span><b>Folder from picker</b><small>Android folder</small></span><button type="button" class="btn t">Remove</button>';d.querySelector('button').onclick=function(){d.remove()};$('edfl').appendChild(d)};
$('edadbset').onclick=aopen;
$('educ').onchange=function(){document.getElementById('eduf').hidden=!this.checked};
$('edicmore').onclick=function(){bs('imod',true)};
$('edmn').oninput=function(){if(DETAIL){ENV[DETAIL].name=this.value;$('edname').textContent=this.value}};
var CFT={name:'Machine name',icon:'Icon',autostart:'Auto-start',lang:'Language',timezone:'Timezone',mounts:'Mounts',cache:'Cache',autoclean:'Auto-cleanup',port:'Code Server port',netshare:'Network sharing',proxy:'Proxy',adb:'ADB',user:'User',ssh:'SSH access',bg:'Background execution',priority:'Process priority',debug:'Debug mode',logs:'View logs',backup:'Backup',reset:'Reset environment'};
document.querySelectorAll('.cfgrow').forEach(function(r){r.onclick=function(){var c=r.dataset.c;$('cfgt').textContent=CFT[c]||'Configuration';document.querySelectorAll('.cfgsec').forEach(function(s){s.hidden=s.dataset.c!==c});bs('cfgmod',true)}});
document.querySelectorAll('.pgroup').forEach(function(g){g.querySelectorAll('.pc').forEach(function(b){b.onclick=function(){g.querySelectorAll('.pc').forEach(function(x){x.setAttribute('aria-pressed',x===b)})}})});
$('edssh').onchange=function(){document.getElementById('edsshp').hidden=!this.checked};
$('edcache').onclick=function(){toast('Cache cleared')};
$('edlogs').onclick=function(){toast('Opening logs')};
$('edbexp').onclick=function(){toast('Backup exported')};
$('edbimp').onclick=function(){toast('Choose a backup to import')};
$('edreset').onclick=function(){toast('Environment reset — confirm to continue')};
var reorder=false;
function enterReorder(){$('envs').classList.add('reorder');$('rdone').hidden=false;reorder=true}
function exitReorder(){reorder=false;$('envs').classList.remove('reorder');$('rdone').hidden=true;document.querySelectorAll('.env').forEach(function(e){e.classList.remove('drg');e.dataset.sup=''})}
$('rdone').onclick=exitReorder;
function attachEnvDrag(env){
  var lp=null,dragging=false,sx=0,sy=0;
  env.addEventListener('pointerdown',function(e){
    if(e.target.closest('.ab')||e.target.closest('.mn'))return;
    if(e.button!==undefined&&e.button!==0)return;
    sx=e.clientX;sy=e.clientY;
    if(reorder){dragging=true;env.classList.add('drg');try{env.setPointerCapture(e.pointerId)}catch(x){}return}
    lp=setTimeout(function(){lp=null;env.dataset.sup='1';enterReorder();dragging=true;env.classList.add('drg');try{env.setPointerCapture(e.pointerId)}catch(x){}},450)
  });
  env.addEventListener('pointermove',function(e){
    if(!dragging){if(lp&&(Math.abs(e.clientX-sx)>8||Math.abs(e.clientY-sy)>8)){clearTimeout(lp);lp=null}return}
    var sibs=[].slice.call(env.parentNode.children).filter(function(c){return c.classList.contains('env')&&c!==env});
    var over=sibs.filter(function(c){var r=c.getBoundingClientRect();return e.clientY<r.top+r.height/2})[0];
    if(over)env.parentNode.insertBefore(env,over);else env.parentNode.appendChild(env);
  });
  env.addEventListener('pointerup',function(e){if(lp){clearTimeout(lp);lp=null}if(dragging){dragging=false;env.classList.remove('drg');try{env.releasePointerCapture(e.pointerId)}catch(x){}}});
  env.addEventListener('pointercancel',function(){if(lp){clearTimeout(lp);lp=null}dragging=false;env.classList.remove('drg')});
}
function openFs(){$('fsmod').classList.add('on');renderFs('/home/user')}
function renderFs(path){var fp=$('fspath'),fl=$('fslist');fp.innerHTML=crumbHtml(path);fp.querySelectorAll('[data-p]').forEach(function(s){s.onclick=function(){renderFs(s.dataset.p)}});fl.innerHTML='';var items=(path==='/'?[]:[{n:'..',t:'up'}]).concat(FILES[path]||[]);items.forEach(function(f){var b=document.createElement('button');b.className='fitem';var ic=f.t==='folder'?'<svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z"/></svg>':f.t==='up'?'<svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>':'<svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/></svg>';b.innerHTML=ic+'<div><b>'+f.n+'</b>'+(f.m?'<small>Mounted</small>':'')+'</div>'+(f.s?'<span class="fs">'+f.s+'</span>':'');b.onclick=function(){if(f.t==='folder'){var np=(path==='/'?'':path)+'/'+f.n;if(!FILES[np])FILES[np]=[];renderFs(np)}else if(f.t==='up'){var pp=path.split('/').slice(0,-1).join('/')||'/';renderFs(pp)}else toast('Mockup: no file viewer')};fl.appendChild(b)})}
function openOv(id){var E=ENV[id];$('ovt').textContent=E.name;$('ovmod').classList.add('on')}
$('custterm').onclick=function(){bs('tmod',true)};
$('stopall').onclick=function(){bs('stopmod',true)};
$('stopconfirm').onclick=function(){bs('stopmod',false);bs('ovmod',false);var s=$('ovstatus');if(s)s.textContent='Stopped';toast('All processes stopped')};
document.querySelectorAll('#sesslist .sx').forEach(function(b){b.onclick=function(){var it=b.closest('.sessitem');if(it)it.remove();toast('Session closed')}});
$('helpname').onclick=function(){var c=$('cmdname');c.hidden=!c.hidden};
document.querySelectorAll('[data-dyn]').forEach(function(x){x.onclick=function(){r.style.removeProperty('--h');$('wheel').style.background='conic-gradient(#2bb673,#3b82f6,#a855f7,#ef7d3a,#2bb673)';document.querySelectorAll('[data-h],[data-mode],[data-dyn],#wheel').forEach(function(z){z.setAttribute('aria-pressed',z===x)})}});
$('wheel').onclick=function(){$('wheelin').click()};
$('wheelin').oninput=function(){var hex=this.value,rr=parseInt(hex.slice(1,3),16)/255,gg=parseInt(hex.slice(3,5),16)/255,bb=parseInt(hex.slice(5,7),16)/255,mx=Math.max(rr,gg,bb),mn=Math.min(rr,gg,bb),d=mx-mn,h=0;if(d!==0){if(mx===rr)h=((gg-bb)/d)%6;else if(mx===gg)h=(bb-rr)/d+2;else h=(rr-gg)/d+4;h*=60;if(h<0)h+=360}r.style.setProperty('--h',Math.round(h));$('wheel').style.background=hex;document.querySelectorAll('[data-h],[data-mode],[data-dyn],#wheel').forEach(function(z){z.setAttribute('aria-pressed',z===$('wheel'))})};
$('themebtn').onclick=function(){bs('thememod',true)};
$('colorbtn').onclick=function(){$('wheel').click()};
$('bgdyn').onclick=function(){clearVars();r.removeAttribute('data-theme');document.querySelectorAll('#bgdyn,#bghard').forEach(function(z){z.setAttribute('aria-pressed',z.id==='bgdyn')})};
$('bghard').onclick=function(){r.setAttribute('data-theme','dark');clearVars();r.style.setProperty('--surface','#000');r.style.setProperty('--sheet','#1c1c1c');r.style.setProperty('--card','#262626');r.style.setProperty('--on-surface','#fff');r.style.setProperty('--on-var','#c2c2c2');r.style.setProperty('--outline','#5c5c5c');r.style.setProperty('--outline-v','#333');document.querySelectorAll('#bgdyn,#bghard').forEach(function(z){z.setAttribute('aria-pressed',z.id==='bghard')})};
$('accbtn').onclick=function(){bs('accmod',true)};
