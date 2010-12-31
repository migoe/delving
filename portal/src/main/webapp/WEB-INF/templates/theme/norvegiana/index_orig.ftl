<#compress>
    <#assign thisPage = "index.html"/>
    <#include "includeMarcos.ftl"/>

<@addHeader "${portalDisplayName}", "",["index.js","cycle/jquery.cycle.min.js"],[]/>

<section id="leftbar" class="grid_3" role="complimentary">
       <div class="slideshow">
        <#list randomItems as item>
            <a href="/${portalName}/record/${item.id}.html"><img src="${item.thumbnail}" /></a>
        </#list>
        </div>
</section>

<section id="info">

</section>

<@addFooter/>

</#compress>

