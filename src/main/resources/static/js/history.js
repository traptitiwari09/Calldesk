/* ---------------------------------------------------------------------------
   history.html - a read-only table of past calls.
   --------------------------------------------------------------------------- */

const message = document.getElementById('message');
const table = document.getElementById('table');
const rows = document.getElementById('rows');
const empty = document.getElementById('empty');

(async function start() {
	const user = await requireLogin();
	if (!user) return;

	document.getElementById('who').textContent = user.email;
	wireLogout();
	await loadHistory();
}());

document.getElementById('refresh').addEventListener('click', () => loadHistory());

async function loadHistory() {
	try {
		render(await apiGet('/api/calls'));
	} catch (error) {
		showMessage(message, error.message);
	}
}

function render(calls) {
	table.hidden = calls.length === 0;
	empty.hidden = calls.length > 0;
	rows.innerHTML = calls.map(rowHtml).join('');
}

function rowHtml(call) {
	return `
		<tr>
			<td>${escapeHtml(call.contactName)}</td>
			<td class="num">${escapeHtml(call.displayNumber)}</td>
			<td>${escapeHtml(formatWhen(call.createdAt))}</td>
			<td class="num">${escapeHtml(formatDuration(call.durationSeconds))}</td>
			<td>${statusHtml(call)}</td>
		</tr>`;
}

/**
 * Twilio's own status words are shown as-is so they match the Twilio console,
 * which makes debugging a failed call much easier.
 */
function statusHtml(call) {
	const label = escapeHtml(call.status.replace('-', ' '));
	const badge = `<span class="badge ${badgeClass(call.status)}">${label}</span>`;

	// A failure reason is worth showing inline - it is usually the whole answer.
	if (call.errorMessage) {
		return `${badge}<div class="row-sub" style="margin-top:4px">${escapeHtml(call.errorMessage)}</div>`;
	}
	return badge;
}

function badgeClass(status) {
	if (status === 'completed') return 'badge-good';
	if (['failed', 'busy', 'no-answer', 'canceled'].includes(status)) return 'badge-bad';
	return 'badge-neutral';
}
