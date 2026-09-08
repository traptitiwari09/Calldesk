/* ---------------------------------------------------------------------------
   Shared helpers. Loaded by every page before its own script.
   --------------------------------------------------------------------------- */

/**
 * Calls the backend and unwraps the JSON.
 *
 * Every endpoint returns {"error": "..."} when something goes wrong, so this
 * turns that into a thrown Error and each page only needs one catch block.
 */
async function api(path, options = {}) {
	const response = await fetch(path, {
		headers: { 'Content-Type': 'application/json' },
		...options,
	});

	// 204 and friends have no body to parse.
	const text = await response.text();
	const data = text ? JSON.parse(text) : {};

	if (!response.ok) {
		throw new Error(data.error || 'Something went wrong. Please try again.');
	}
	return data;
}

const apiGet = (path) => api(path);
const apiPost = (path, body) => api(path, { method: 'POST', body: JSON.stringify(body ?? {}) });
const apiDelete = (path) => api(path, { method: 'DELETE' });

/** Shows a message in a .msg element, or hides it when text is empty. */
function showMessage(element, text, kind = 'error') {
	if (!text) {
		element.hidden = true;
		return;
	}
	element.textContent = text;
	element.className = `msg msg-${kind}`;
	element.hidden = false;
}

/**
 * Runs an async action while showing a "busy" label on the button.
 * Stops double-clicks turning into two phone calls.
 */
async function withBusy(button, busyLabel, action) {
	const original = button.textContent;
	button.disabled = true;
	button.textContent = busyLabel;
	try {
		return await action();
	} finally {
		button.disabled = false;
		button.textContent = original;
	}
}

/** Initials for the avatar circle: "John Smith" -> "JS". */
function initialsOf(name) {
	const parts = name.trim().split(/\s+/).filter(Boolean);
	if (parts.length === 0) return '?';
	if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
	return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

/** A stable colour per name, so the same contact always looks the same. */
function colourOf(name) {
	const palette = ['#4f46e5', '#0891b2', '#7c3aed', '#c2410c', '#059669', '#be185d', '#0369a1', '#65a30d'];
	let hash = 0;
	for (const character of name) {
		hash = (hash * 31 + character.charCodeAt(0)) | 0;
	}
	return palette[Math.abs(hash) % palette.length];
}

/** Escapes text before it goes into innerHTML - a contact could be named "<script>". */
function escapeHtml(value) {
	return String(value ?? '').replace(/[&<>"']/g, (character) => ({
		'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
	}[character]));
}

/** "2:14 PM, 23 Aug" - short and readable. */
function formatWhen(isoString) {
	const date = new Date(isoString);
	return date.toLocaleString(undefined, {
		hour: 'numeric', minute: '2-digit',
		day: 'numeric', month: 'short',
	});
}

/** 47 -> "47s", 132 -> "2m 12s". */
function formatDuration(seconds) {
	if (seconds === null || seconds === undefined) return '-';
	if (seconds < 60) return `${seconds}s`;
	return `${Math.floor(seconds / 60)}m ${seconds % 60}s`;
}

/**
 * Redirects to the login page unless someone is signed in.
 * Returns the logged-in user so pages can show their name.
 */
async function requireLogin() {
	try {
		return await apiGet('/api/auth/me');
	} catch {
		window.location.href = '/login.html';
		return null;
	}
}

/** Wires up the "Log out" button present in the sidebar of every signed-in page. */
function wireLogout() {
	const button = document.getElementById('logout');
	if (!button) return;
	button.addEventListener('click', async () => {
		await apiPost('/api/auth/logout').catch(() => {});
		window.location.href = '/login.html';
	});
}
