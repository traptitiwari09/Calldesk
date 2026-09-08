/* ---------------------------------------------------------------------------
   Drives both login.html and signup.html. Only one of the two forms exists
   on any given page, so each block is guarded.
   --------------------------------------------------------------------------- */

const message = document.getElementById('message');

/* ------------------------------------------------------------------- log in */

const loginForm = document.getElementById('login-form');

if (loginForm) {
	loginForm.addEventListener('submit', async (event) => {
		event.preventDefault();
		showMessage(message, '');

		const submit = document.getElementById('submit');

		await withBusy(submit, 'Logging in...', async () => {
			try {
				await apiPost('/api/auth/login', {
					email: document.getElementById('email').value,
					password: document.getElementById('password').value,
				});
				window.location.href = '/dashboard.html';
			} catch (error) {
				showMessage(message, error.message);
			}
		});
	});
}

/* ------------------------------------------------------------------ sign up */

const signupForm = document.getElementById('signup-form');

if (signupForm) {
	signupForm.addEventListener('submit', async (event) => {
		event.preventDefault();
		showMessage(message, '');

		const submit = document.getElementById('submit');

		await withBusy(submit, 'Creating account...', async () => {
			try {
				await apiPost('/api/auth/signup', {
					name: document.getElementById('name').value,
					email: document.getElementById('email').value,
					phoneNumber: document.getElementById('phoneNumber').value,
					password: document.getElementById('password').value,
				});
				// Signup logs you straight in, so go to the dashboard.
				window.location.href = '/dashboard.html';
			} catch (error) {
				showMessage(message, error.message);
			}
		});
	});
}
