// 注入jQuery
var jqScriptEl = document.createElement('script');
jqScriptEl.setAttribute('src', 'https://cdn.staticfile.org/jquery/1.10.2/jquery.min.js');
document.querySelector('head').appendChild(jqScriptEl);

$('body').css({'padding': '0', 'margin': '0'});
$('body')[0].innerHTML = '';
$('title')[0].innerHTML='Loading';

var bodyInner = `
<div style="width: 100%; height:100vh; background-color: #222728; color: #ffffff; line-height: 100vh;">
    <p style="margin: 0; text-align: center; font-size: 18px">
        <span id="executor-loading-time" style="color: red; font-weight: bold"></span>
        s后自动打开第
        <span id="executor-loading-index" style="color: red; font-weight: bold"></span>
        份问卷
    </p>
</div>
`;

$('body')[0].innerHTML = bodyInner;