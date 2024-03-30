// 注入jQuery
var jqScriptEl = document.createElement('script');
jqScriptEl.setAttribute('src', 'https://cdn.staticfile.org/jquery/1.10.2/jquery.min.js');
document.querySelector('body').appendChild(jqScriptEl);

var ulEl = `<ul id="executor-window" style="background-color: #00de76;width: 300px;position: fixed;top: 10px;right: 10px;padding: 10px;z-index: 9999;box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.2);">
                                <li>当前问卷：第<span id="executor-window-index" style="color: red; font-weight: bold">0</span>份</li>
                                <li id="executor-window-address">归属地：</li>
                                <li id="executor-window-isp">运营商：</li>
                                <li id="executor-window-ip">IP：</li>
                                <li>注意：<span id="executor-window-time" style="color: red; font-weight: bold">0</span>s后自动提交问卷</li>
                                <li><span id="executor-window-msg" style="color: red;"></span></li>
                           </ul>`;
// 注入卡片
var cardEl = $(ulEl);
$('body').append(cardEl);

$.ajax({
    async: true,
    url: 'https://qifu-api.baidubce.com/ip/local/geo/v1/district',
    type: 'GET',
    dataType: 'json',
    timeout: 30000,
    success: (response) => {
        var data = response?.data;
        var address = data?.country + ' ' + data?.prov + ' ' + data?.city + ' ' + data?.district;
        $(ulEl).find('#executor-window-address')[0].innerHTML = '归属地：' + address;
        $(ulEl).find('#executor-window-isp')[0].innerHTML = '运营商：' + data?.isp;
        $(ulEl).find('#executor-window-ip')[0].innerHTML = 'IP：' + response?.ip;
        if (!data) {
            $(ulEl).find('#executor-window-msg')[0].innerHTML = '获取IP信息失败：' + JSON.stringify(response);
        }
    },
    error: (response) => {
        $(ulEl).find('#executor-window-msg')[0].innerHTML = '获取IP信息失败：' + JSON.stringify(response);
    }
});






