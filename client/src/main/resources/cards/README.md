# Card Images

This directory should contain poker card images for the game UI.

## Where to Download Card Images

You can download free poker card images from:

1. **OpenGameArt.org**
   - https://opengameart.org/content/playing-cards-vector-png
   - Free SVG/PNG playing card set

2. **Wikimedia Commons**
   - https://commons.wikimedia.org/wiki/Category:SVG_playing_cards
   - Public domain card images

3. **Kenney.nl**
   - https://kenney.nl/assets/boardgame-pack
   - Free game assets including cards

## Naming Convention

Cards should be named with the format: `{rank}{suit}.png`

Examples:
- `AH.png` - Ace of Hearts
- `2D.png` - Two of Diamonds
- `KS.png` - King of Spades
- `TC.png` - Ten of Clubs

Where:
- Rank: 2-9, T (Ten), J (Jack), Q (Queen), K (King), A (Ace)
- Suit: H (Hearts), D (Diamonds), C (Clubs), S (Spades)

## Note

The current implementation displays card text only. Once you add images to this directory, you can update the GameScreen.java to load and display the actual card images instead of text labels.
