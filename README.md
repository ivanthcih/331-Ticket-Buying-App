# 331-HW7-Ticket-Buying-App
A full-stack ticket queueing and purchasing system built with a Java/Javalin backend and a vanilla JavaScript frontend.

# Overview
The app simulates buying tickets to a limited-capacity venue (the "shopping center"):
Users declare how many tickets they want to buy.
If there's room, they're admitted immediately; otherwise they join a first-come, first-served waiting line.
Users at the front of the line are admitted only once their declared quantity fits in the currently free capacity — smaller requests behind a larger one still have to wait their turn.
Once admitted, a user has a countdown deadline to complete a purchase (for 1 up to their reserved quantity) or they're evicted and sent to the back of the line.
When a slot frees up (purchase, manual leave, or timeout), the app automatically admits the next eligible waiters from the front of the line.
Users can view a profile page with purchase history and rename themselves.
An open admin dashboard shows live queue length, capacity usage, and the next person in line, and supports inserting a new waiter at an arbitrary position in the queue.
Key Engineering Highlights
Custom generic data structure — Designed and implemented GenericQueue<V>, a key-value queue supporting name-based lookup, insertion, removal, and reordering, with explicit Big-O time and space complexity requirements enforced per method (O(1), O(log n), and O(n) operations).
Rigorous specification testing — Wrote a specification-based test suite that must pass against any correct implementation of the interface, exercising the queue with multiple generic value types (GenericQueue<String>, GenericQueue<Integer>).
Correctness invariants — Maintained abstraction functions (AF) and representation invariants (RI) with a checkRep() method to guard internal consistency as the implementation evolved.
REST API design — Built a JSON API in Java using Javalin (no server-side HTML rendering), with clear separation between backend logic and frontend presentation.
Stateful frontend without a framework — Implemented dynamic, multi-view UI (join → wait → shop → purchase/profile/admin) in plain JavaScript using fetch, DOM manipulation, and page-visibility toggling — no external frontend framework.
Defensive backend design — All API routes validate input and return structured error responses instead of crashing; eviction/promotion logic runs at the top of each request handler to keep server state consistent without background threads.
