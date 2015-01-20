ip-locator
==========

<p>
2015-01-15 增加IP爬虫功能，对国内的每一个C段ip，进行扫描，然后对结果进行合并，得到国内ip库
</p>

<p>
用于IP定位，包含了纯真IP库（ipseeker），和本地文本IP库两种（iplocator）。<br/>
IP库需要经常保持更新才能保证准确度。
</p>
<p>
本地ip库的数据来源：<br/>
  1.从<a href="http://ftp.apnic.net/stats/">http://ftp.apnic.net/stats/</a>获取最新的delegated-*-latest文件<br/>
  2.分配给CN之外的各个IP段，通过简称对应国家名称写入IP库<br/>
  3.分配给CN的IP段，通过ip.taobao.com及其他ip网站爬取一遍，细分出每个省份等信息，写入IP库<br/>
  4.利用RadixTree把本地文件读入内存<br/>
<p/>
<p>
将delegated-*-latest文件转换为本地IP库的程序，参见ipdb_creator
</P>
