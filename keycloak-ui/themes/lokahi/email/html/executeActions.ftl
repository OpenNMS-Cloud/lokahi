<#outputformat "plainText">
<#assign requiredActionsText><#if requiredActions??><#list requiredActions><#items as reqActionItem>${msg("requiredAction.${reqActionItem}")}<#sep>, </#sep></#items></#list></#if></#assign>
</#outputformat>

<#import "template.ftl" as layout>
<@layout.emailLayout>
  <p>
    ${kcSanitize(msg("actionsText"))?no_esc}
  </p>
  <div style="
        display: block;
        width: 100%;
      ">
    <a href="${link}" 
      style="
        text-decoration: none;
        display: block; 
        background: #273180; 
        color: white; 
        padding: 7px 15px; 
        border-radius: 5px; 
        font-weight: 600;
        width: 140px;
        margin: auto;
        margin-top: 40px;
        margin-bottom: 40px;
        text-transform: uppercase;
      ">
       ${kcSanitize(msg("actionsBtnText", requiredActionsText))?no_esc}
    </a>
  </div>
  <p>
    ${kcSanitize(msg("actionsExpiryText", linkExpirationFormatter(linkExpiration)))?no_esc}
  </p>
</@layout.emailLayout>