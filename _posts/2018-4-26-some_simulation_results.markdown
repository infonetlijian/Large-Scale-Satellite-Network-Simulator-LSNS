---
layout: post
title:  "Several simulation results"
date:   2018-05-10 11:49:00
categories: main
---


Recently, we make some simulations to test/verity our routing module and transmission module in LSNS.
<br>
Routing module: comparison between static cluster routing algorithm and dynamic cluster routing algorithm:
<br>
![PacketSize_vs_Latency](https://github.com/infonetlijian/ONE-Extended-Simulator/raw/gh-pages/images/Simulation_Results/PacketSize_vs_Latency.png)<br>
Transmission module: According to different interruption probability of inter-satellite links, different retransmission is required to improve delivery probability:
<br>
![ReTransTimes_vs_DeliveryProbability](https://github.com/infonetlijian/ONE-Extended-Simulator/raw/gh-pages/images/Simulation_Results/ReTransTimes_vs_DeliveryProbability.png)<br>
<br>
We make 'MessagesBuffer' as cache region, and make a comparison between two-layer satellite network (LEO+MEO) and three-layer satellite network (LEO+MEO+GEO).
![BufferSize_vs_DeliveryProbability](https://github.com/infonetlijian/ONE-Extended-Simulator/raw/gh-pages/images/Simulation_Results/BufferSize_vs_DeliveryProbability.png)
<br>
**Contact**：<br>
E-mail: lijian9@mail.ustc.edu.cn, hclu@ustc.edu.cn<br>
[**Lab**](http://if.ustc.edu.cn)<br>
<br>
<br>






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

<br>
<br>

<script>
  (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
  })(window,document,'script','https://www.google-analytics.com/analytics.js','ga');

  ga('create', 'UA-101909927-1', 'auto');
  ga('send', 'pageview');

</script>
