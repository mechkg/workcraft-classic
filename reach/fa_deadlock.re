let requests = tset{T "ra+", T "rb+", T "rc+"} {
	forall t in TRANSITIONS\requests { ~@t }
	&
	exists p in PLACES { $p ^ is_init p }
}