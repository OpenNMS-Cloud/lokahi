<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true displayMessage=!messagesPerField.existsError('username'); section>
    <#if section = "header">
        ${msg("emailForgotTitle")} </br>
        <span class="typography-body-small">${msg("emailForgotTitleMsg")}<span>
    <#elseif section = "form">
        <form id="hs-reset-password-form" action="${url.loginAction}" method="post">
            <div>
                <#if messagesPerField.existsError('username')>
                    <span id="hs-input-error" aria-live="polite">
                        ${kcSanitize(messagesPerField.get('username'))?no_esc}
                    </span>
                </#if>

                <input placeholder="${msg('placeholderEmail')}" type="text" id="username" name="username" class="feather-input" autofocus value="${(auth.attemptedUsername!'')}" aria-invalid="<#if messagesPerField.existsError('username')>true</#if>"/>
            </div>
            <div>
                <div id="hs-form-buttons">
                    <button class="btn hover focus btn-primary" type="submit">${msg("doSubmit")}</button>
                </div>
                <div id="hs-back-link">
                    <div class="typography-button">
                        <span><a class="link" href="${url.loginUrl}">${kcSanitize(msg("backToLogin"))?no_esc}</a></span>
                    </div>
                </div>
            </div> 
        </form>
    <#elseif section = "info" >
        <#if realm.duplicateEmailsAllowed>
            ${msg("emailInstructionUsername")}
        <#else>
            ${msg("emailInstruction")}
        </#if>
    </#if>
</@layout.registrationLayout>
