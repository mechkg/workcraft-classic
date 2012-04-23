// first check for the trivial case when some signal has 
// both triggers of the same sign and triggers of the opposite sign
exists s in LOCAL {
    let pos = exists e in ev s, f in trig e { is_plus f <-> is_plus e },
        neg = exists e in ev s, f in trig e { is_plus f ^ is_plus e } {
		pos & neg
	}
} | 
exists s in LOCAL {
    let pos = exists e in ev s, f in trig e { is_plus f <-> is_plus e },
        neg = exists e in ev s, f in trig e { is_plus f ^ is_plus e } {
		// the pos and neg cannot hold together - 
		// checked by the trivial case above
		pos & s' & ~s'' | neg & ~s' & s''
	}
} & 
forall ss in SIGNALS { $ss -> $$ss }
	