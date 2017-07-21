---
layout: post
title:  "关于在JAVA下的文本文件读写问题"
date:   2016-12-21 15:22:51 
categories: main
---

这两天被JAVA下关于读写文本文件的操作给弄伤了，怎么调都有问题，其实读写无非就是用Reader和Writer下的类进行操作，但是我想边读边写一个文本文件，以实现近修改配置文本文件(作为程序执行时的Setting文件)中的部分行，但就是不停出错。今天终于发现问题就出在“边读边写同一个文件”这里，首先给出一个正确操作的代码：

```Java
public void write(){
	inputStream = Settings.class.getClass().getResourceAsStream(FILEPATH);
	reader = new BufferedReader(new InputStreamReader(inputStream,"GBK"));
	while ((read = reader.readLine()) != null) {
		content.add(read);
	}
	reader.close();
	outputStream = new FileOutputStream(file);
	writer = new PrintWriter(outputStream);
	for (String s : content){
		if (s.startsWith(name)){
			writer.println(name + " = " + value);
	}
	else
		writer.println(s);
	}
	writer.close();
}
```

可以注意到的是，对于同一个文件File，reader和writer同时只能够开启一个，所以边读边写这种方式肯定是会出错的，正确的方式应该是先读取文件内容到自定义的数据结构中，在关闭reader之后，再通过对之前存储下来的文本内容进行判断，然后重新覆盖回写，同时注意，PrintWriter是属于字符型的writer，会有一个缓冲区，所以在执行完写入命令后，需要通过close()命令来真正实现对文件的写入（或者是flush()）。




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
