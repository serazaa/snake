# Update 1.0
# Snake — Enhanced

A polished, modernized take on your original Swing Snake game, with new features, cleaner structure, and nicer visuals.

## Design choices
- 24×24 at 25px: keeps cells visually readable while allowing snappy turns.
- Timer on EDT: trivial to integrate with Swing; plenty for a small grid game.
- No threads: avoids synchronization bugs for beginners tweaking the code.

## What's new
- Themes (cycle with **T**)
- Grid toggle (**G**)
- Wrap-through walls (**W**)
- Obstacles toggle (**O**)
- Pause/Resume (**P**) and Restart (**R**)
- Golden Apple power-up (+5 points, sometimes spawns)
- Adjustable speed (**+ / -**) and auto-speed-up every 5 apples
- High score persisted via `java.util.prefs.Preferences`
- HUD + Help overlay (**H** to hide/show)
- Menu bar with common actions
- Smooth rounded visuals, gradient background, and cute snake eyes 👀

## Controls
- Move: Arrow keys or WASD
- Start: **Space**
- Pause/Resume: **P**
- Restart: **R**
- Toggle Grid: **G**
- Cycle Theme: **T**
- Wrap Walls: **W**
- Obstacles: **O**
- Speed: **+** / **-**
- Toggle Help: **H**

## Project layout
