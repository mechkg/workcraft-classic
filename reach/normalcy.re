exists s in LOCAL {
    let pos = exists e in ev s, f in trig e { is_plus f <-> is_plus e },
        neg = exists e in ev s, f in trig e { is_plus f ^ is_plus e } {
            pos & neg | pos & s' & ~s'' | neg & ~s' & s''
        }
} & 
forall ss in SIGNALS { $ss -> $$ss }
