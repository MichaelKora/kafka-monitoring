All the runs and HPA configs were tested with the stair-like spiky workload:
1. 10_000 msg per minute, all sent at once (spiky)
2. and CPU intense consumer strategy (for each incoming message: create an array of 50_000 random ints, then sort this array)
