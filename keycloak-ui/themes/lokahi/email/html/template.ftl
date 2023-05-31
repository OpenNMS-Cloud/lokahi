<#macro emailLayout>
<html>
<body>
    <div style="
            display: block;
            background: #F4F7FC; 
            width: 450px; 
            height: 400px; 
            padding: 50px;
            padding-top: 1px;
        ">

        <p style=
                "font-weight: bold; 
                color: black;
                font-size: 20px;
                text-align: center;
            ">
            OpenNMS
        </p>

        <div style="
                background: white; 
                padding: 40px; 
                height: 190px;
            ">
            <#nested>
        </div>


        <p style="
                color: gray;
                font-size: 10px;
                text-align: center;
            ">
            &#xA9;${msg("opennmsGroupText")}
        </p>

        <p style="
                color: gray;
                font-size: 10px;
                text-align: center;
            ">
            ${msg("addressText")}
        </p>
    </div>
</body>
</html>
</#macro>