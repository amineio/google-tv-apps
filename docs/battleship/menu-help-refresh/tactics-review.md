# Tactics review

The four tips in the supplied photograph fit this game with the limits below.
The photograph is a content reference. Do not bundle the photograph or its surrounding personal scene.

The review used the supplied photograph, `Ship.kt`, and `ShipPlacement.kt`.
Ships occupy straight horizontal or vertical runs of 5, 4, 3, 3, and 2 cells.
Placement prevents overlap but permits touching ships.

## Correct patterns

A hunt pattern searches for an unknown ship before a hit.
Choose one checkerboard color while the two-cell ship remains afloat.
For zero-based row and column indices, mark cells where `(row + column) % 2 == 0`.
Every horizontal or vertical pair contains one marked cell.
Every ship therefore crosses the pattern, but one hit does not sink a ship.

After the two-cell destroyer is confirmed sunk, all remaining ships have at least three cells.
The hunt can then use `(row + column) % 3 == 0`.
Every horizontal or vertical run of three cells contains one marked cell.
Shift the marked columns by one cell on each new row.
Do not mark the same columns in every row.
Do not select every third cell in a continuous row-major sequence across the ten-column board.
Those alternatives leave valid ship positions untouched.

Use the pattern for new searches only. Finish unresolved hits before the next search.
After a hit, try untried cells directly above, below, left, and right.
After aligned hits, follow that line until the ship sinks.
Touching ships can produce hits from different ships, so alignment is a clue rather than a guarantee.
A hit does not grant an extra turn.

Prefer unexplored space that can still fit a remaining ship.
Do not treat the border around a sunk ship as empty water because ships can touch.
Do not skip an unresolved hit to follow the open-water tip.

## Native diagram rules

Create the diagrams with native grid drawing. Use equal square cells.
Keep hunt candidates visually distinct from actual hits and misses.
The targeting sight marks the current candidate. It does not mark a shot result.

The first diagram uses this checkerboard mask:

```text
X . X . X
. X . X .
X . X . X
. X . X .
X . X . X
```

The second diagram shows one central hit and four orthogonal candidates.
Do not highlight diagonal neighbors.

The third diagram shows a separate sunk two-cell ship and this three-cell hunt mask:

```text
X . . X . .
. . X . . X
. X . . X .
X . . X . .
. . X . . X
. X . . X .
```

Keep the sunk ship separate from the mask if it overlaps a hunt marker.
The fourth diagram shows a sight within a large unexplored area and existing shot results elsewhere.
Do not draw a forbidden border around a sunk ship.

## Short translated copy

Use four equal cards. Each card contains one diagram, one title, and one short body.
Keep these tips below the basic controls and objective.

### English

1. **Search a checkerboard**: Search one color to find a hidden ship.
2. **Follow a hit**: Try the four neighboring cells. Follow the line after aligned hits.
3. **Two-cell ship sunk?**: Search every third cell. Shift the pattern on each row.
4. **Choose open water**: Aim where a remaining ship can fit. Ships can touch.

### Dutch

1. **Zoek in een dambordpatroon**: Doorzoek één kleur om een verborgen schip te vinden.
2. **Volg een treffer**: Probeer de vier aangrenzende vakjes. Volg de lijn bij treffers op een rij.
3. **Schip van twee vakjes gezonken?**: Zoek om de drie vakjes. Verschuif het patroon op elke rij.
4. **Kies open water**: Richt waar een overgebleven schip past. Schepen mogen elkaar raken.

### French

1. **Cherchez en damier**: Visez une seule couleur pour trouver un navire caché.
2. **Suivez une touche**: Essayez les quatre cases voisines. Suivez la ligne si les touches sont alignées.
3. **Navire de deux cases coulé ?**: Visez une case sur trois. Décalez le motif à chaque ligne.
4. **Visez l’eau libre**: Visez un espace adapté aux navires restants. Les navires peuvent se toucher.

## Review limits

The coverage argument follows directly from the ship lengths and axis-aligned placement rules.
It guarantees a pattern intersection with each intact ship, not an optimal shot order or a win.
This review does not claim that the photographs describe the source or rights of the original online material.
