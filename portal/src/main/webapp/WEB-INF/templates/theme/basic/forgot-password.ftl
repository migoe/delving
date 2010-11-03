<#import "spring.ftl" as spring />
<#assign thisPage = "forgot-password.html"/>

<#include "includeMarcos.ftl">

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

<h2>Wachtwoord aanvragen</h2>

<form action="forgot-password.html" method="POST" accept-charset="UTF-8">
    <table>
        <tr>
            <td><label for="email"><@spring.message 'EmailAddress_t' /></label></td>
            <td><input id="email" type="text" name="email" value="" maxlength="50"></td>
        </tr>
        <tr>
            <td></td>
            <td><input id="submit_forgot" name="submit_login" type="submit" value="Aanvragen"/></td>
        </tr>
    </table>
</form>

<#if state == "success">
<p id="forgotSuccess" class="success">
<@spring.message 'AnEmailHasBeenSentTo_t' />: <span class="fg-gold">${email}</span>.
<@spring.message 'PleaseFollowTheLinkProvided_t' />.  <!-- TODO change message -->
</p>
</#if>
<#if state == "formatFailure">
<@spring.message 'Error_t' />!<br/><@spring.message 'EmailFormatError_t' />.
</#if>
<#if state == "nonexistentFailure">
<@spring.message 'Error_t' />!<br/>EmailDoesntExist_t <!-- TODO add message -->
</#if>

</section>


<@addFooter/>

