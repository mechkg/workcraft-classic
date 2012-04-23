forall e in EVENTS {
	let pre_e = pre e {
		exists f in pre pre_e { ~$f } |
		exists f in post pre_e \ CUTOFFS { $f }
	}
}
