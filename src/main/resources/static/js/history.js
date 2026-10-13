/* 
   history.html - a read-only table of past calls.
    */

const message = document.getElementById('message');
const table = document.getElementById('table');
const rows = document.getElementById('rows');
const empty = document.getElementById('empty');
const filters = document.getElementById('filters');

/** ALL | INBOUND | OUTBOUND - sent to the API, not applied in the browser. */
let direction = 'ALL';

filters.addEventListener('click', (event) => {
	const button = event.target.closest('button[data-direction]');
	if (!button) return;

	direction = button.dataset.direction;
	filters.querySelectorAll('button[data-direction]')
		.forEach((other) => other.classList.toggle('active', other === button));
	loadHistory();
});


document.getElementById('refresh').addEventListener('click', () => loadHistory());
async function loadHistory() {
	try {
		const query = direction === 'ALL' ? '' : `?direction=${direction}`;
		render(await apiGet(`/api/calls${query}`));
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
			<td>${directionHtml(call)}</td>
			<td>${escapeHtml(call.displayName)}</td>
			<td class="num">${numbersHtml(call)}</td>
			<td>${escapeHtml(formatWhen(call.createdAt))}</td>
			<td class="num">${escapeHtml(formatDuration(call.durationSeconds))}</td>
			<td>${statusHtml(call)}</td>
		</tr>`;
}

/** An arrow plus a word: the arrow reads at a glance, the word survives a screen reader. */
function directionHtml(call) {
	return call.inbound
		? '<span class="badge badge-in">&#8600; Incoming</span>'
		: '<span class="badge badge-out">&#8599; Outgoing</span>';
}

/** Both ends of the call, caller first. Older rows may lack one end - show what we have. */
function numbersHtml(call) {
	if (!call.displayFrom || !call.displayTo) {
		return escapeHtml(call.displayNumber);
	}
	return `${escapeHtml(call.displayFrom)} <span aria-label="to">&rarr;</span> ${escapeHtml(call.displayTo)}`;
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
	if (status === 'completed' || status === 'returned') return 'badge-good';
	if (['failed', 'busy', 'no-answer', 'canceled'].includes(status)) return 'badge-bad';
	return 'badge-neutral';
}
