<#import "spring_form_macros.ftl" as spring />
<#assign thisPage = "change-password.html"/>
<#assign pageId = "cp"/>
<#include "delving-macros.ftl">

<@addHeader "Norvegiana", "",[],[]/>

<section class="grid_3">
    <header id="branding">
        <a href="/${portalName}/" title=""/>
        <img src="/${portalName}/${portalTheme}/images/norvegiana.jpg" alt="Norvegiana"/>
        </a>
        <h1 class="large">${portalDisplayName}</h1>
    </header>
</section>


<section role="main" class="grid_9">

<div id="main">

    <h1><@spring.message 'ForgotPassword_t' /></h1>

    <form id="regForm" action="change-password.html" method="post">

        <input type="hidden" name="token" value="${command.token}" />
        <input type="hidden" name="email" value="${command.email}" /><#-- disabled email field below is not submitted so we need this hidden field -->
     <div class="grid_4">
        <fieldset id="pt1">
            <legend><span>Step </span>1. <span>: Email details</span> </legend>
            <label for="email"><@spring.message 'EmailAddress_t' /></label>
            <input type="text" id="email" name="email" disabled="true" tabindex="5"  value="${command.email}" style="background:#eaeaea;"/>

        </fieldset>
     </div>
        <div class="grid_4 alpha">
        <fieldset id="pt2">
            <legend><span>Step </span>2. <span>: Password</span></legend>
            <label for="password"><@spring.message 'Password_t' /></label>
            <input type="password" id="password" name="password" tabindex="5"  value=""/>
            <@spring.bind "command.password" />
            <#list spring.status.errorMessages as error> <i>${error}</i> <br> </#list>

            <label for="password2"><@spring.message 'RepeatPassword_t' /></label>
            <input type="password" id="password2" name="password2" tabindex="5"  value=""/>
            <@spring.bind "command.password2" />
            <#list spring.status.errorMessages as error> <i>${error}</i> <br> </#list>
        </fieldset>
       </div>
        <div class="grid_4 omega">
        <fieldset id="pt3">
          <legend><span>Step </span>3. <span>: Password</span></legend>
          <br/>
          <input id="submit" type="submit" name="submit" tabindex="6" value="<@spring.message 'Send_t' /> &raquo;" class="button"/>
        </fieldset>
       </div>
  </form>
</div>

</section>

<@addFooter/>

