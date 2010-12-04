<#compress>
    <#if imagePathList??>
        <#if javascript>
            var tinyMCEImageList = new Array(
            <#list imagePathList as imagePath>
                 ["${imagePath}","${imagePath}"]<#if imagePath_has_next>,</#if>
            </#list>
            );

        <#else>
            <#assign thisPage = "static-page.dml"/>
            <#assign pageId = "static"/>
            <#include "includeMarcos.ftl">

            <@addHeader "${portalDisplayName}", "",[],[]/>


            <section role="main" class="main">

                <h1><@spring.message '_cms.administration.images' /></h1>

                <div class="grid_8">
                    <h2><@spring.message '_cms.existing.images' /></h2>
                     <table summary="List of existing images" class="user-options">
                        <#list imagePathList as imagePath>
                            <tr>
                                <td width="50"><img src="${imagePath}" alt="thumbnail" height="20"/></td>
                                <td width="300"><a href="${imagePath}?edit=true"><span class="ui-icon ui-icon-image"></span>${imagePath}</a></td>
                                <td width="85"><a href="${imagePath}?edit=true"><span class="ui-icon ui-icon-pencil"></span><@spring.message '_cms.edit' /></a></td>
                                <td width="85">
                                     <a class="delete" id="delete_${imagePath_index}" href="${imagePath}"><span class="ui-icon ui-icon-trash"></span><@spring.message '_cms.delete' /></a>
                                </td>
                            </tr>
                        </#list>
                    </table>
                </div>


                <div class="grid_4">

                    <h2><@spring.message '_cms.image.create' /></h2>
                    <form method="POST" action="/${portalName}/images/_.img" enctype="multipart/form-data">
                        <table>
                            <tr>
                                <td><input type="file" name="file" size="30"/></td>
                            </tr>
                            <tr>
                                <td><input type="submit" name="submit" value="<@spring.message '_cms.upload' />"></td>
                            </tr>
                        </table>
                    </form>
                    
                    <script type="text/javascript">
                        function createImage(){
                            var name = $("#imgName").attr("value");
                            var ext = $("#imgExt :selected").text();
                            var pName = $("#pName").attr("value");
                            var makeURL = pName+name+ext+".img";
                            window.location.href=makeURL+"?edit=true";
                        }

                        $("a.delete").click(function(){
                            var target = $(this).attr("id");
                            var targetURL = $(this).attr("href");
                            var confirmation = confirm("<@spring.message '_cms.image.delete.question' />")
                            if(confirmation){
                                $.ajax({
                                    url: targetURL+"?edit=false&delete=true",
                                    type: "GET",
                                    success: function(data) {
                                        window.location.reload();
                                    },
                                    error: function(data) {
                                        alert("<@spring.message '_cms.image.delete.fail' />");
                                    }
                                });
                            }
                            return false;
                        });
                    </script>
                </div>
            </section>

            <div class="clear"></div>
            <@addFooter/>
        </#if>
    <#else>
        <#assign thisPage = "static-image.img"/>
        <#assign pageId = "static"/>

        <#include "includeMarcos.ftl">

        <@addHeader "${portalDisplayName}", "",[],[]/>

            <section id="sidebar" class="grid_3" role="complementary">
                <header id="branding" role="banner">
                    <a href="/${portalName}/" title=""/>
                    <img src="/${portalName}/${portalTheme}/images/norvegiana.jpg" alt="${portalDisplayName}"/>
                    </a>
                    <h1 class="large">${portalDisplayName}</h1>
                </header>
            </section>


            <section role="main">

                <header>
                    <h1><@spring.message '_cms.administration.images' /></h1>
                </header>

                <div class="grid_5">
                    <#if imageExists>
                        <img src="/${portalName}/${imagePath}" alt="${imagePath}" style="max-width:100%"/><br/>
                        ${imagePath}
                    <#else>
                        <p><@spring.message '_cms.image.not.exist' /></p>
                    </#if>
                </div>

                <#--<div class="clear"></div>-->

                <div class="grid_4">
                    <#if edit??>
                        <#if edit>
                            <div id="pageForm">
                                <form method="POST" enctype="multipart/form-data">
                                    <table>
                                        <tr>
                                            <td width="200"><@spring.message '_cms.image.choose' /></td>
                                            <td><input type="file" name="file" size="30"/></td>
                                        </tr>
                                        <tr>
                                            <td></td>
                                            <td><input type="submit" name="submit" value="<@spring.message '_cms.upload' />"></td>
                                        </tr>
                                    </table>
                                </form>
                            </div>
                            <div id="pageForm2">
                                <form method="POST">
                                    <table>
                                        <tr>
                                            <td width="200">Nieuwe afbeelding URL</td>
                                            <td><input type="newPath" name="newPath" value="${imagePath}" size="30"/></td>
                                        </tr>
                                        <tr>
                                            <td></td>
                                            <td><input type="submit" name="submit" value="Afbeelding hernoemen"></td>
                                        </tr>
                                    </table>
                                </form>
                            </div>
                        <#else>
                            <p><a href="/${portalName}/${imagePath}?edit=true" class="button"><@spring.message '_cms.image.change' /></a></p>
                        </#if>
                        <p><a href="/${portalName}/_.img" class="button"><@spring.message '_cms.image.list' /></a></p>
                    </#if>

                </div>

            </section>

        <@addFooter/>
    </#if>
</#compress>
