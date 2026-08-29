let currentUser = null; // name of the logged-in user (set after join/find)

// pages
const pages = {
    queue:    document.getElementById('page-queue'),
    waiting:  document.getElementById('page-waiting'),
    shopping: document.getElementById('page-shopping-center'),
    profile:  document.getElementById('page-profile'),
    admin:    document.getElementById('page-admin'),
};

function showPage(name) {
    for (const [key, el] of Object.entries(pages)) {
        el.hidden = key !== name;
    }
}

// show queue page by default on load
showPage('queue');

// nav
document.getElementById('nav-queue').addEventListener('click', () => showPage('queue'));
document.getElementById('nav-profile').addEventListener('click', () => showPage('profile'));
document.getElementById('nav-admin').addEventListener('click', () => {
    showPage('admin');
    doAdminRefresh();
});

function showError(context, detail) {
    console.error(context, detail);
}

const joinNameEl = document.getElementById('join-name');
const joinQuantityEl = document.getElementById('join-quantity');
const queueMsgEl = document.getElementById('queue-msg');

document.getElementById('join-btn').addEventListener('click', async () => {
    const name = joinNameEl.value.trim();
    const quantity = parseInt(joinQuantityEl.value);
    if (!name) { queueMsgEl.textContent = 'Please enter a name.'; queueMsgEl.hidden = false; return; }

    let resp, data;
    try {
        resp = await fetch('/api/join', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, quantity }),
        });
        data = await resp.json();
    } catch (err) { showError('could not reach /api/join', err); return; }

    if (!resp.ok) {
        queueMsgEl.textContent = data.error || 'Could not join.';
        queueMsgEl.hidden = false;
        return;
    }

    queueMsgEl.hidden = true;
    currentUser = name;
    updateLoggedName();

    if (data.status === 'shopping') {
        populateShopping(data);
        showPage('shopping');
    } else {
        populateWaiting(data);
        showPage('waiting');
    }
});

document.getElementById('find-btn').addEventListener('click', async () => {
    const name = joinNameEl.value.trim();
    if (!name) { queueMsgEl.textContent = 'Please enter a name.'; queueMsgEl.hidden = false; return; }

    let resp, data;
    try {
        resp = await fetch('/api/status?name=' + encodeURIComponent(name));
        data = await resp.json();
    } catch (err) { showError('could not reach /api/status', err); return; }

    if (!resp.ok) {
        queueMsgEl.textContent = data.error || 'Could not find your spot.';
        queueMsgEl.hidden = false;
        return;
    }

    queueMsgEl.hidden = true;
    currentUser = name;
    updateLoggedName();

    if (data.status === 'shopping') {
        populateShopping(data);
        showPage('shopping');
    } else if (data.status === 'waiting') {
        populateWaiting(data);
        showPage('waiting');
    } else {
        queueMsgEl.textContent = 'You are not currently in the queue or shopping center.';
        queueMsgEl.hidden = false;
    }
});

// waiting-page
function populateWaiting(data) {
    document.getElementById('position-queue').textContent = data.position;
    document.getElementById('waiting-time').textContent   = data.waitSeconds;
    document.getElementById('quantity-queue').textContent = data.quantity;
    document.getElementById('wait-new-quantity').value    = data.quantity;
}

document.getElementById('wait-refresh-btn').addEventListener('click', async () => {
    if (!currentUser) return;
    let resp, data;
    try {
        resp = await fetch('/api/status?name=' + encodeURIComponent(currentUser));
        data = await resp.json();
    } catch (err) { showError('could not reach /api/status', err); return; }
    if (!resp.ok) { showError('/api/status error', data); return; }

    if (data.status === 'shopping') {
        populateShopping(data);
        showPage('shopping');
    } else {
        populateWaiting(data);
    }
});

document.getElementById('wait-adjust-btn').addEventListener('click', async () => {
    if (!currentUser) return;
    const quantity = parseInt(document.getElementById('wait-new-quantity').value);
    let resp, data;
    try {
        resp = await fetch('/api/adjust', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: currentUser, quantity }),
        });
        data = await resp.json();
    } catch (err) { showError('could not reach /api/adjust', err); return; }
    if (!resp.ok) { showError('/api/adjust error', data); return; }

    if (data.status === 'shopping') {
        populateShopping(data);
        showPage('shopping');
    } else {
        populateWaiting(data);
    }
});

document.getElementById('wait-leave-btn').addEventListener('click', async () => {
    if (!currentUser) return;
    let resp, data;
    try {
        resp = await fetch('/api/leave', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: currentUser }),
        });
        data = await resp.json();
    } catch (err) { showError('could not reach /api/leave', err); return; }
    if (!resp.ok) { showError('/api/leave error', data); return; }

    currentUser = null;
    updateLoggedName();
    queueMsgEl.textContent = 'You have left the queue.';
    queueMsgEl.hidden = false;
    showPage('queue');
});

// shopping-page
function populateShopping(data) {
    document.getElementById('shop-deadline').textContent = data.secondsLeft;
    document.getElementById('shop-quantity').textContent = data.quantity;
    document.getElementById('shop-purchase-quantity').value = 1;
    document.getElementById('shop-purchase-quantity').max   = data.quantity;
}

document.getElementById('shop-refresh-btn').addEventListener('click', async () => {
    if (!currentUser) return;
    let resp, data;
    try {
        resp = await fetch('/api/status?name=' + encodeURIComponent(currentUser));
        data = await resp.json();
    } catch (err) { showError('could not reach /api/status', err); return; }
    if (!resp.ok) { showError('/api/status error', data); return; }

    if (data.status === 'shopping') {
        populateShopping(data);
    } else {
        // timed out — send back to queue
        currentUser = null;
        updateLoggedName();
        queueMsgEl.textContent = 'Your shopping time expired. Please re-join.';
        queueMsgEl.hidden = false;
        showPage('queue');
    }
});

document.getElementById('shop-purchase-btn').addEventListener('click', async () => {
    if (!currentUser) return;
    const quantity = parseInt(document.getElementById('shop-purchase-quantity').value);
    let resp, data;
    try {
        resp = await fetch('/api/purchase', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: currentUser, quantity }),
        });
        data = await resp.json();
    } catch (err) { showError('could not reach /api/purchase', err); return; }
    if (!resp.ok) { showError('/api/purchase error', data); return; }

    currentUser = null;
    updateLoggedName();
    queueMsgEl.textContent = `Thank you for your purchase of ${quantity} ticket(s)!`;
    queueMsgEl.hidden = false;
    showPage('queue');
});

document.getElementById('shop-leave-btn').addEventListener('click', async () => {
    if (!currentUser) return;
    let resp, data;
    try {
        resp = await fetch('/api/leave', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: currentUser }),
        });
        data = await resp.json();
    } catch (err) { showError('could not reach /api/leave', err); return; }
    if (!resp.ok) { showError('/api/leave error', data); return; }

    currentUser = null;
    updateLoggedName();
    queueMsgEl.textContent = 'You have left the shopping center.';
    queueMsgEl.hidden = false;
    showPage('queue');
});

// profile page
document.getElementById('lookup-btn').addEventListener('click', async () => {
    const name = document.getElementById('lookup-name').value.trim();
    if (!name) return;

    let resp, data;
    try {
        resp = await fetch('/api/profile?name=' + encodeURIComponent(name));
        data = await resp.json();
    } catch (err) { showError('could not reach /api/profile', err); return; }
    if (!resp.ok) { showError('/api/profile error', data); return; }

    document.getElementById('found-name').textContent = data.name;

    const list = document.getElementById('profile-purchases');
    list.innerHTML = '';
    for (const p of data.purchases) {
        const li = document.createElement('li');
        li.textContent = p; // backend should format e.g. "5/29/2026, 3:38:02 AM: 2 tickets"
        list.appendChild(li);
    }

    document.getElementById('profile-info').hidden = false;
});

document.getElementById('change-btn').addEventListener('click', async () => {
    const oldName = document.getElementById('lookup-name').value.trim();
    const newName = document.getElementById('change-name').value.trim();
    if (!oldName || !newName) return;

    let resp, data;
    try {
        resp = await fetch('/api/rename', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: oldName, newName }),
        });
        data = await resp.json();
    } catch (err) { showError('could not reach /api/rename', err); return; }
    if (!resp.ok) { showError('/api/rename error', data); return; }

    document.getElementById('lookup-name').value = newName;
    document.getElementById('found-name').textContent = data.name;
    if (currentUser === oldName) {
        currentUser = newName;
        updateLoggedName();
    }
});

// admin page
async function doAdminRefresh() {
    let resp, data;
    try {
        resp = await fetch('/api/admin');
        data = await resp.json();
    } catch (err) { showError('could not reach /api/admin', err); return; }
    if (!resp.ok) { showError('/api/admin error', data); return; }

    document.getElementById('admin-total-capacity').textContent = data.totalCapacity;
    document.getElementById('admin-used-capacity').textContent = data.usedCapacity;
    document.getElementById('admin-available-capacity').textContent = data.totalCapacity - data.usedCapacity;
    document.getElementById('admin-queue-length').textContent = data.queueLength;
    document.getElementById('admin-queue-tickets').textContent = data.queueTickets;
    document.getElementById('admin-next').textContent  = data.nextUp || '(queue empty)';
}

document.getElementById('admin-refresh-btn').addEventListener('click', doAdminRefresh);

document.getElementById('admin-insert-btn').addEventListener('click', async () => {
    const name = document.getElementById('admin-insert-name').value.trim();
    const after = document.getElementById('admin-insert-after').value.trim();
    const quantity = parseInt(document.getElementById('admin-insert-quantity').value);
    if (!name) return;

    let resp, data;
    try {
        resp = await fetch('/api/admin/insert', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, after, quantity }),
        });
        data = await resp.json();
    } catch (err) { showError('could not reach /api/admin/insert', err); return; }
    if (!resp.ok) { showError('/api/admin/insert error', data); return; }

    doAdminRefresh();
});

// just for shopping center page
function updateLoggedName() {
    const navName = document.getElementById('nav-name');
    navName.textContent = currentUser ? `(${currentUser})` : '';
}