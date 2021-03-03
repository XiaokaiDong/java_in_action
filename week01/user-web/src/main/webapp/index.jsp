<head>
<jsp:directive.include
	file="/WEB-INF/jsp/prelude/include-head-meta.jspf" />
<title>My Home Page</title>
</head>
<body>
	<div class="container-lg">
		<!-- Content here -->
		Hello,World 2021
	</div>
    <div class="main">
		<div class="title">
			<span>用户注册</span>
		</div>
		<div class="title-msg">
			<span>请输入用户名和密码</span>
		</div>
		<form class="login-form" method="post" novalidate action="/user/register">
			<div class="input-content">
				<div>
					<input type="text" autocomplete="off" placeholder="用户名" name="userName" required/>
				</div>

				<div style="margin-top: 16px">
					<input type="password" autocomplete="off" placeholder="登陆密码" name="password" required maxlength="32" />
				</div>

				<div>
					<input type="text" autocomplete="off" placeholder="邮箱" name="email" required/>
				</div>

				<div>
					<input type="text" autocomplete="off" placeholder="手机号" name="phoneNumber" required/>
				</div>

			</div>

			<div style="text-align: center">
				<button type="submit" class="enter-btn">注册</button>
			</div>


		</form>
	</div>
</body>