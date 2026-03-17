// ！！！关键：替换成你的后端接口地址（本地启动的端口）
const BASE_URL = "http://localhost:8083/api/users";

// 注册函数
function register() {
    // 获取输入值
    const username = document.getElementById("reg-username").value;
    const password = document.getElementById("reg-password").value;
    const phone = document.getElementById("reg-phone").value;
    const resultDom = document.getElementById("reg-result");

    // 清空之前的结果
    resultDom.innerHTML = "";
    resultDom.className = "";

    // 简单校验
    if (!username || !password) {
        resultDom.className = "error";
        resultDom.innerHTML = "用户名和密码不能为空！";
        return;
    }

    // 调用后端注册接口
    fetch(`${BASE_URL}/register`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            username: username,
            password: password,
            phone: phone
        })
    })
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                resultDom.className = "success";
                resultDom.innerHTML = "注册成功！";
                // 清空输入框
                document.getElementById("reg-username").value = "";
                document.getElementById("reg-password").value = "";
                document.getElementById("reg-phone").value = "";
            } else {
                resultDom.className = "error";
                resultDom.innerHTML = "注册失败：" + data.msg;
            }
        })
        .catch(error => {
            resultDom.className = "error";
            resultDom.innerHTML = "注册出错：请检查后端服务是否启动！";
            console.error("注册错误：", error);
        });
}

// 登录函数
function login() {
    // 获取输入值
    const username = document.getElementById("login-username").value;
    const password = document.getElementById("login-password").value;
    const resultDom = document.getElementById("login-result");

    // 清空之前的结果
    resultDom.innerHTML = "";
    resultDom.className = "";

    // 简单校验
    if (!username || !password) {
        resultDom.className = "error";
        resultDom.innerHTML = "用户名和密码不能为空！";
        return;
    }

    // 调用后端登录接口
    fetch(`${BASE_URL}/login`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            username: username,
            password: password
        })
    })
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                resultDom.className = "success";
                resultDom.innerHTML = "登录成功！Token：" + data.data;
                // 清空输入框
                document.getElementById("login-username").value = "";
                document.getElementById("login-password").value = "";
            } else {
                resultDom.className = "error";
                resultDom.innerHTML = "登录失败：" + data.msg;
            }
        })
        .catch(error => {
            resultDom.className = "error";
            resultDom.innerHTML = "登录出错：请检查后端服务是否启动！";
            console.error("登录错误：", error);
        });
}