/* ---------------------------------------------------------------------------
   dashboard.html - list contacts, add and delete them, and place calls.
   --------------------------------------------------------------------------- */

const message = document.getElementById('message');
const list = document.getElementById('contact-list');
const empty = document.getElementById('empty');
const addPanel = document.getElementById('add-panel');

/* --------------------------------------------------------------- page setup */

(async function start() {
	const user = await requireLogin();
	if (!user) return;

	document.getElementById('who').textContent = user.email;
	wireLogout();
	await loadContacts();
}());

/* ------------------------------------------------------------ rendering */

async function loadContacts() {
	try {
		render(await apiGet('/api/contacts'));
	} catch (error) {
		showMessage(message, error.message);
	}
}

function render(contacts) {
	empty.hidden = contacts.length > 0;
	list.innerHTML = contacts.map(rowHtml).join('');

	list.querySelectorAll('[data-call]').forEach((button) => {
		button.addEventListener('click', () => placeCall(button));
	});
	list.querySelectorAll('[data-delete]').forEach((button) => {
		button.addEventListener('click', () => deleteContact(button));
	});
}

function rowHtml(contact) {
	const name = escapeHtml(contact.name);
	return `
		<li class="list-row">
			<div class="avatar" style="background:${colourOf(contact.name)}">${escapeHtml(initialsOf(contact.name))}</div>
			<div class="row-body">
				<div class="row-title">${name}</div>
				<div class="row-sub">${escapeHtml(contact.displayNumber)}</div>
			</div>
			<div class="row-actions">
				<button class="btn btn-call" type="button"
				        data-call data-id="${contact.id}" data-name="${name}">Call</button>
				<button class="btn btn-ghost-danger" type="button"
				        data-delete data-id="${contact.id}" data-name="${name}"
				        title="Delete contact">Delete</button>
			</div>
		</li>`;
}

/* ------------------------------------------------------------ placing a call */

async function placeCall(button) {
	showMessage(message, '');

	await withBusy(button, 'Calling...', async () => {
		try {
			await apiPost('/api/calls', { contactId: Number(button.dataset.id) });
			showMessage(
				message,
				`Calling ${button.dataset.name} - your phone should ring in a few seconds. Answer it to be connected.`,
				'success');
		} catch (error) {
			showMessage(message, error.message);
		}
	});
}

/* ----------------------------------------------------------- add and delete */

document.getElementById('toggle-add').addEventListener('click', () => {
	addPanel.hidden = !addPanel.hidden;
	if (!addPanel.hidden) {
		document.getElementById('name').focus();
	}
});

document.getElementById('add-form').addEventListener('submit', async (event) => {
	event.preventDefault();
	showMessage(message, '');

	const nameInput = document.getElementById('name');
	const phoneInput = document.getElementById('phoneNumber');

	await withBusy(document.getElementById('add-submit'), 'Saving...', async () => {
		try {
			await apiPost('/api/contacts', { name: nameInput.value, phoneNumber: phoneInput.value });
			nameInput.value = '';
			phoneInput.value = '';
			addPanel.hidden = true;
			await loadContacts();
		} catch (error) {
			showMessage(message, error.message);
		}
	});
});

async function deleteContact(button) {
	if (!window.confirm(`Delete ${button.dataset.name}?`)) return;

	showMessage(message, '');
	try {
		await apiDelete(`/api/contacts/${button.dataset.id}`);
		await loadContacts();
	} catch (error) {
		showMessage(message, error.message);
	}
}


/* ---------------------------------------------------------------------------
   Incoming call queue.

   Shared across every agent - an inbound call belongs to the business, not to
   whoever happens to be logged in. First agent to click Call back claims it.
   --------------------------------------------------------------------------- */

const queueList = document.getElementById('queue-list');
const queueEmpty = document.getElementById('queue-empty');
const queueCount = document.getElementById('queue-count');

async function loadQueue() {
	try {
		renderQueue(await apiGet('/api/calls/inbound'));
	} catch (error) {
		showMessage(message, error.message);
	}
}

function renderQueue(calls) {
	const waiting = calls.filter((call) => call.status !== 'returned');

	queueEmpty.hidden = calls.length > 0;
	queueCount.hidden = waiting.length === 0;
	queueCount.textContent = `${waiting.length} waiting`;
	queueList.innerHTML = calls.map(queueRow).join('');
}

function queueRow(call) {
	const known = Boolean(call.contactName);
	const returned = call.status === 'returned';

	return `
		<li class="row${returned ? ' row-muted' : ''}">
			<div class="avatar" style="background:${colourOf(call.displayName)}">
				${escapeHtml(known ? initialsOf(call.contactName) : '☎')}
			</div>
			<div class="row-main">
				<div class="row-title">${escapeHtml(call.displayName)}</div>
				<div class="row-sub">
					${escapeHtml(call.displayNumber)} &middot; ${escapeHtml(formatWhen(call.createdAt))}
					${known ? '' : ' &middot; <span class="muted">not in contacts</span>'}
				</div>
			</div>
			${returned
				? '<span class="badge badge-good">Returned</span>'
				: `<button class="btn btn-primary" data-callback="${call.id}" type="button">Call back</button>`}
		</li>`;
}

queueList.addEventListener('click', async (event) => {
	const button = event.target.closest('[data-callback]');
	if (!button) return;

	// Send only the call id - the server decides which number that means.
	await withBusy(button, 'Calling...', async () => {
		try {
			const call = await apiPost('/api/calls', { callbackForCallId: Number(button.dataset.callback) });
			showMessage(message, `Calling ${call.displayName} - answer your phone.`, 'ok');
			await loadQueue();
		} catch (error) {
			showMessage(message, error.message);
		}
	});
});

document.getElementById('refresh-queue').addEventListener('click', loadQueue);

/* Dial a number that is not in the address book. */
document.getElementById('dial-form').addEventListener('submit', async (event) => {
	event.preventDefault();
	const input = document.getElementById('dial-number');
	if (!input.value.trim()) return;

	const button = document.getElementById('dial-submit');
	await withBusy(button, 'Calling...', async () => {
		try {
			const call = await apiPost('/api/calls', { phoneNumber: input.value });
			showMessage(message, `Calling ${call.displayName} - answer your phone.`, 'ok');
			input.value = '';
		} catch (error) {
			showMessage(message, error.message);
		}
	});
});

loadQueue();
