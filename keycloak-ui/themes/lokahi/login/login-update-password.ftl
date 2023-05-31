<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('password','password-confirm'); section>
    <#if section = "header">
        ${msg("updatePasswordTitle")}
    <#elseif section = "form">
        <form action="${url.loginAction}" method="post">

            <#--  From KC base templates, appears to be hack to avoid autocomplete in New/Confim password inputs  -->
            <input type="text" id="username" name="username" value="${username}" autocomplete="username"
                   readonly="readonly" style="display:none;"/>
            <input type="password" id="password" name="password" autocomplete="current-password" style="display:none;"/>

            <#--  New password  -->
            <div>
                <input type="password" id="password-new" name="password-new" class="feather-input"
                        autofocus autocomplete="new-password" placeholder="${msg('passwordNew')}"
                        aria-invalid="<#if messagesPerField.existsError('password','password-confirm')>true</#if>"
                />

                <#if messagesPerField.existsError('password')>
                    <span id="hs-input-error" aria-live="polite">
                        ${kcSanitize(messagesPerField.get('password'))?no_esc}
                    </span>
                </#if>
            </div>

            <#-- Confirm Password -->
            <div>
                <input type="password" id="password-confirm" name="password-confirm"
                        class="feather-input" placeholder="${msg('passwordConfirm')}"
                        autocomplete="new-password"
                        aria-invalid="<#if messagesPerField.existsError('password-confirm')>true</#if>"
                />

                <#if messagesPerField.existsError('password-confirm')>
                    <span id="hs-input-error" aria-live="polite">
                        ${kcSanitize(messagesPerField.get('password-confirm'))?no_esc}
                    </span>
                </#if>

            </div>

            <div>
                <#if isAppInitiatedAction??>
                    <div class="checkbox">
                        <label><input type="checkbox" id="logout-sessions" name="logout-sessions" value="on" checked> ${msg("logoutOtherSessions")}</label>
                    </div>
                </#if>
            </div>
     
            <#--  Submit  -->
            <div>
                <#if isAppInitiatedAction??>
                    <button class="btn hover focus btn-primary" type="submit">${msg("doSubmit")}</button>
                    <button class="btn hover focus btn-secondary" type="submit" name="cancel-aia" value="true" />${msg("doCancel")}</button>
                <#else>
                    <button class="btn hover focus btn-primary" type="submit">${msg("doSubmit")}</button>
                </#if>
            </div>
  
        </form>
    </#if>
</@layout.registrationLayout>