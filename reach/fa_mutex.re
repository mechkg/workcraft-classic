let h1 = $S"ra" & $S"ga", 
    h2 = $S"rb" & $S"gb",
    h3 = $S"rc" & $S"gc" {
	h1 & h2 | h2 & h3 | h1 & h3
}