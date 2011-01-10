<#assign thisPage = "mine.html">

<#compress>


<#include "includeMarcos.ftl">

<@addHeader "${portalDisplayName}", "",["mine.js"],["mine.css"]/>

<section id="main" class="main grid_12">


    <div id="savedItems">
        <ul>
            <li><a href="#tab-0" onclick="$.cookie('mine-tabs', '0', { expires: 1, path: '/${portalName}' });" title="<@spring.message '_mine.user.information' />"><span><@spring.message '_mine.user.information' /></span></a></li>
            <li><a href="#tab-1" onclick="$.cookie('mine-tabs', '1', { expires: 1, path: '/${portalName}' });" title="<@spring.message '_mine.saved.items'/>"><span><@spring.message '_mine.saved.items'/></span></a></li>
            <li><a href="#tab-2" onclick="$.cookie('mine-tabs', '2', { expires: 1, path: '/${portalName}' });" title="<@spring.message '_mine.saved.searches'/>"><span><@spring.message '_mine.saved.searches'/></span></a></li>
        </ul>
       <div id="tab-0">
          <table summary="table with user information" id="tbl-user" >
              <caption><@spring.message '_mine.user.information' /></caption>
            <tr>
                <td width="150">
                    <@spring.message '_mine.username' />:
                </td>
                <td>${user.userName}</td>
            </tr>
            <tr>
              <td><@spring.message '_mine.email.address'/>:</td>
              <td>${user.email}<input type="hidden" name="umail" id="umail" value="${user.email}"/> </td>
            </tr>
            <#if user.registrationDate??>
            <tr>
              <td><@spring.message '_mine.user.registration.date'/>:</td>
              <td>${user.registrationDate?date}</td>
            </tr>
                <tr>
                    <td></td>
                    <td><button id="rem-user" class="btn-strong delete"><@spring.message '_cms.delete' /></button> </td>
                </tr>
            </#if>
        </table>
         <#assign test = "Jan 6, 1999"/>
       </div>
        <div id="tab-1">
            <table class="zebra" summary="list with saved items">
                <caption><@spring.message '_mine.saved.items'/></caption>
                <tbody>
                <#if user.items?size &gt; 0>
                    <#list user.items as item>
                        <#assign rowId = "item_row_"+item.index/>
                        <tr id="${rowId}">
                            <td width="60">
                                <a href="/${portalName}/record/${item.europeanaId}.html">
                                 <#if useCache="true">
                                    <#--<img class="thumb" id="img_${item_index}" align="middle" src="${cacheUrl}id=${item.thumbnail}&size=BRIEF_DOC" alt="Click for more information" width="50" style="float:left" onerror="showDefaultSmall(this,'${item.docType}')"/>-->
                                 <#else>
                                    <#if item.docType??>
                                      <img class="thumb" align="middle" src="${item.thumbnail}" alt="Click for more information" width="50" style="float:left" onerror="showDefaultSmall(this,'${item.docType}')"/>
                                    <#else>
                                      <img class="thumb" align="middle" src="${item.thumbnail}" alt="Click for more information" width="50" style="float:left" onerror="showDefaultSmall(this,'${item.docType}')"/>
                                    </#if>
                                 </#if>
                                </a>
                            </td>
                            <td valign="top" class="item-info">
                                <a href="/${portalName}/record/${item.europeanaId}.html">
                                    <strong><@stringLimiter "${item.getTitle()}" "50"/></strong>
                                </a>
                                <br/>
                                <@spring.message '_search.field.creator'/>: <em>${item.getAuthor()}</em>
                                <br/>
                                <@spring.message '_mine.date.saved'/>: <em>${item.getDateSaved()?datetime}</em>

                            </td>
                            <td width="60">
                                <a href="#" class="fg-button ui-state-default fg-button-icon-left ui-corner-all" onclick="removeSavedItem(${item.index?string('0')},'${rowId}');"><span class="ui-icon ui-icon-trash"></span><@spring.message '_mine.delete'/></a>
                            </td>
                        </tr>
                    </#list>

                <#else>
                    <tr><td><@spring.message '_mine.nosaveditems'/></td></tr>
                </#if>
                </tbody>
            </table>
        </div>
       <div id="tab-2">
            <table class="zebra" summary="list with saved searches">
                <caption><@spring.message '_mine.saved.searches'/></caption>
                <#if user.searches?size &gt; 0>
                    <#list user.searches as search>
                       <#assign rowId = "search_row_"+search_index/>
                        <tr id="${rowId}">
                            <td width="5"><a href="/${portalName}/brief-doc.html?${search.query}"><span class="ui-icon ui-icon-search"></span></a></td>
                            <td valign="top" class="item-info">
                                <a href="/${portalName}/brief-doc.html?${search.query}">${search.queryString}</a><br/>
                                <@spring.message '_mine.date.saved'/>: <em>${search.dateSaved?datetime}</em>
                            </td>
                            <td width="60"><a href="#" class="fg-button ui-state-default fg-button-icon-left ui-corner-all" onclick="removeSavedSearch(${search.index?string('0')},'${rowId}');"><span class="ui-icon ui-icon-trash"></span><@spring.message '_mine.delete'/></a></td>
                        </tr>
                    </#list>
                <#else>
                    <tr><td><@spring.message '_mine.nosavedsearches'/></td></tr>
                </#if>
            </table>
        </div>
    </div>
</section>
<script type="text/javascript">
    $('button#rem-user').click(function() {
        showConfirm("notification", "<@spring.message '_cms.user.remove.confirm'/>", "removeUser('" + $("input#umail").val() + "')")
    });
</script>

    <@addFooter/>

</#compress>
