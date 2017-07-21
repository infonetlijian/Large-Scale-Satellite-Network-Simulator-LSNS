---
layout: post
title:  "关于java项目从eclipse移植到intellij idea的抓狂之旅"
date:   2017-04-25 22:01:00
categories: main
---

又一次作死，尝试把之前的java项目从eclipse移植到intellij idea，没办法，知乎上一群人吹捧intellij idea，不过对于我而言其实是因为mac平台的eclipse实在是太不好用了……<br>
目前虽然成功了，但是java 3d的模块貌似就是有问题，后面再解决吧……<br>
成功移植经历，[**参照网站**]：(http://m.2cto.com/kf/201608/539241.html)，还有把lib文件单独拿出来和源代码文件夹分开，再单独导入到library下面，同时在run的时候进行configuration里面，虽然没有读出主程序，但直接在名称栏输入core.DTNSim来强制运行，居然运行成功……<br>



<div id="disqus_thread"></div>
<script>

/**
*  RECOMMENDED CONFIGURATION VARIABLES: EDIT AND UNCOMMENT THE SECTION BELOW TO INSERT DYNAMIC VALUES FROM YOUR PLATFORM OR CMS.
*  LEARN WHY DEFINING THESE VARIABLES IS IMPORTANT: https://disqus.com/admin/universalcode/#configuration-variables*/
/*
var disqus_config = function () {
this.page.url = PAGE_URL;  // Replace PAGE_URL with your page's canonical URL variable
this.page.identifier = PAGE_IDENTIFIER; // Replace PAGE_IDENTIFIER with your page's unique identifier variable
};
*/
(function() { // DON'T EDIT BELOW THIS LINE
var d = document, s = d.createElement('script');
s.src = 'https://nathendrake.disqus.com/embed.js';
s.setAttribute('data-timestamp', +new Date());
(d.head || d.body).appendChild(s);
})();
</script>
<noscript>Please enable JavaScript to view the <a href="https://disqus.com/?ref_noscript">comments powered by Disqus.</a></noscript>
