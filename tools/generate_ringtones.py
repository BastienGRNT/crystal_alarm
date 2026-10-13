#!/usr/bin/env python3
"""Synthesizes Crystal Alarm's built-in ringtones (pure Python, no dependency).

Each sound is a seamless loop written as 16-bit mono WAV into app/src/main/res/raw.
Usage: python3 tools/generate_ringtones.py
"""
import math
import random
import struct
import wave
from pathlib import Path

SR = 22050
OUT = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res" / "raw"


def silence(seconds):
    return [0.0] * int(seconds * SR)


def envelope(n, attack=0.005, release=0.005):
    a, r = max(1, int(attack * SR)), max(1, int(release * SR))
    return [min(1.0, i / a, (n - i) / r) for i in range(n)]


def tone(freq, seconds, wave_fn, attack=0.005, release=0.005, decay=None):
    n = int(seconds * SR)
    env = envelope(n, attack, release)
    out = []
    for i in range(n):
        t = i / SR
        amp = env[i] * (math.exp(-t / decay) if decay else 1.0)
        out.append(amp * wave_fn(2 * math.pi * freq * t))
    return out


def sine(phase):
    return math.sin(phase)


def square(phase):
    # Soft square: rich and harsh, but without aliasing clicks.
    return math.tanh(4 * math.sin(phase))


def triangle(phase):
    return 2 / math.pi * math.asin(math.sin(phase))


def mix(*tracks):
    n = max(len(t) for t in tracks)
    return [sum(t[i] for t in tracks if i < len(t)) for i in range(n)]


def normalize(samples, peak):
    m = max(abs(s) for s in samples) or 1.0
    return [s / m * peak for s in samples]


def write(name, samples, peak=0.9):
    OUT.mkdir(parents=True, exist_ok=True)
    data = normalize(samples, peak)
    with wave.open(str(OUT / f"{name}.wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(b"".join(struct.pack("<h", int(s * 32767)) for s in data))
    print(f"{name}.wav  {len(data) / SR:.1f}s")


def hard():
    """Aggressive alternating square bursts, no room to breathe."""
    out = []
    for _ in range(3):
        for k in range(8):
            out += tone(1050 if k % 2 == 0 else 1580, 0.06, square)
            out += silence(0.03)
        out += silence(0.18)
    return out


def forte():
    """Classic digital alarm clock: beep-beep-beep-beep, pause."""
    out = []
    for _ in range(2):
        for _ in range(4):
            out += mix(tone(2000, 0.1, square), [0.3 * s for s in tone(4000, 0.1, sine)])
            out += silence(0.08)
        out += silence(0.45)
    return out


def cloche():
    """Mechanical bell: a hammer striking inharmonic metal partials ~18 times per second."""
    out = []
    partials = [(1.0, 1.0), (2.76, 0.5), (5.4, 0.25), (8.93, 0.12)]
    base = 1000
    for _ in range(2):
        for _ in range(27):
            strike = mix(*[[a * s for s in tone(base * r, 0.055, sine, 0.001, 0.004, decay=0.04)] for r, a in partials])
            out += strike
        out += silence(0.5)
    return out


def calme():
    """Soft marimba-like pentatonic arpeggio."""
    notes = [523.25, 659.25, 783.99, 1046.5, 783.99, 659.25, 587.33, 783.99]
    out = []
    for f in notes:
        note = mix(
            tone(f, 0.5, sine, 0.004, 0.05, decay=0.22),
            [0.35 * s for s in tone(f * 4, 0.5, sine, 0.002, 0.05, decay=0.05)],
        )
        out += note
    out += silence(0.6)
    return out


def aube():
    """Slow warm swells, like light coming through the curtains."""
    chords = [[261.63, 329.63, 392.0, 493.88], [220.0, 261.63, 329.63, 392.0]]
    out = []
    for chord in chords:
        n = int(4.0 * SR)
        swell = []
        for i in range(n):
            t = i / SR
            env = math.sin(math.pi * i / n) ** 2
            s = sum(
                math.sin(2 * math.pi * f * t) + 0.5 * math.sin(2 * math.pi * f * 1.003 * t)
                for f in chord
            )
            shimmer = 0.15 * math.sin(2 * math.pi * chord[-1] * 2 * t) * (0.5 + 0.5 * math.sin(2 * math.pi * 0.5 * t))
            swell.append(env * (s + shimmer))
        out += swell
    return out


def sirene():
    """Rising and falling siren sweep (phase-continuous)."""
    out, phase = [], 0.0
    period = 2.4
    n = int(period * SR) * 2
    for i in range(n):
        t = (i / SR) % period
        x = t / (period / 2) if t < period / 2 else 2 - t / (period / 2)
        freq = 650 + 750 * (0.5 - 0.5 * math.cos(math.pi * x))
        out.append(triangle(phase) + 0.25 * square(phase))
        phase += 2 * math.pi * freq / SR
    return out


if __name__ == "__main__":
    random.seed(1)
    write("ringtone_hard", hard())
    write("ringtone_forte", forte())
    write("ringtone_cloche", cloche())
    write("ringtone_calme", calme(), peak=0.8)
    write("ringtone_aube", aube(), peak=0.8)
    write("ringtone_sirene", sirene())
