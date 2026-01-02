# burp_ai_scan
它通过分析 HTTP 请求/响应数据，结合预训练的 AI 模型，识别传统规则无法发现的复杂安全漏洞（如逻辑漏洞、业务层攻击面等）




## 下载
1.右方 Releases处,下载Export.jar和burp_ai_scan文件夹压缩包,并解压

2.将 阿里云百炼的API Key 加入 系统环境变量(参考阿里云百炼网站文档详情)
<img src="https://mmbiz.qpic.cn/sz_mmbiz_png/CZUj03ETIMh1GWNSB5cRRic5V7Uh6pzJ7M9iat3OESBMcCvXX7z7BnM9nfQpibrOdZ7CHzD9uia3o5LC3oUsIZGLog/640?wx_fmt=png&from=appmsg&watermark=1&tp=webp&wxfrom=5&wx_lazy=1#imgIndex=0">

3.进入burp_ai_scan文件夹 执行
```
pip install -r requirements.txt
```


注:Export.jar是 jdk11及以上可以使用 burp_ai_scan是Python 3.5及以上可以使用



## 快速开始

1.将  Export.jar手动导入 burp extensions

2.该插件记录 burp 所有流量包,搜索可能含有漏洞的数据包并选中,点击复制选中框
<img src="https://mmbiz.qpic.cn/sz_mmbiz_png/CZUj03ETIMh1GWNSB5cRRic5V7Uh6pzJ7akhg8ko1Sa1E4GTV05MOp05M2Y52WXzibIxofSezlibeBsRnCyyazJkA/640?wx_fmt=png&from=appmsg&watermark=1&tp=webp&wxfrom=5&wx_lazy=1#imgIndex=1">

3.将数据粘贴至 burp_AI_scan文件夹的1.txt
<img src="https://mmbiz.qpic.cn/sz_mmbiz_png/CZUj03ETIMh1GWNSB5cRRic5V7Uh6pzJ7b06vowHaIUT64ibITfkNSW4ghYrOXFgy4Fz0ZLlQawtIax2oklAv9Xg/640?wx_fmt=png&from=appmsg&watermark=1&tp=webp&wxfrom=5&wx_lazy=1#imgIndex=2">
4.执行 
```
python burp_ai_scan.py -r 1.txt
```
<img src="https://mmbiz.qpic.cn/sz_mmbiz_png/CZUj03ETIMh1GWNSB5cRRic5V7Uh6pzJ7yvdSG7nHXjzGS6pPBZsMO4MtRLVicNEibGX9tx3tvCUuianeALpQjIlJA/640?wx_fmt=png&from=appmsg&watermark=1&tp=webp&wxfrom=5&wx_lazy=1#imgIndex=3">


5.输出
```
漏洞类型：
双引号联合 SQL 注入

Payload：
demo.name"%20FROM%20sqlite_master--

WAF绕过：
未检测出WAF
```

## Export.jar与TsojanScan联合使用方式:

1.将可以数据包发送给 TsojanScan 进一步测试
<img src="https://mmbiz.qpic.cn/sz_mmbiz_png/CZUj03ETIMh1GWNSB5cRRic5V7Uh6pzJ7ibvGXGa5QgRkcRrfmuic8ibibplEW6vFsSw5M06UyClf9Qb7qzia3ibib7ibhA/640?wx_fmt=png&from=appmsg&watermark=1&tp=webp&wxfrom=5&wx_lazy=1#imgIndex=4">

2.Export插件 获取到TsojanScan的数据(也可搜索url)
<img src="https://mmbiz.qpic.cn/sz_mmbiz_png/CZUj03ETIMh1GWNSB5cRRic5V7Uh6pzJ7KdCZg57v0N1NLnJoCicO9iavPJJqbsibWqCqZbCMorj7hianFhwxufvQiaQ/640?wx_fmt=png&from=appmsg&watermark=1&tp=webp&wxfrom=5&wx_lazy=1#imgIndex=5">

3.选择要复制的请求包和返回包,点击复制选中项

4.将数据粘贴至burp_ai_scan文件夹的1.txt 

5.执行python burp_ai_scan.py -r 1.txt


## Export的5大功能
1.日志:记录来自burp 所有模块流量的数据包
<img src="./1.png">

2.复制:可将 多个数据包 一起复制,格式为 请求包1:... 返回包1:... 请求包2:... 返回包2:.. 适合AI分析数据包,可用于个人AI研究的prompt
<img src=./2.png>
<img src=./3.png>

3.排序:可将 Export.jar 界面的列选项 排序,可排序后查看已选中的复选框
<img src=2.png>

4.搜索功能:搜索关键字
<img src=4.png>



## 原创:公众号-小安全sec

