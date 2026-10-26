#!/usr/bin/env python3
"""Original CC0 sounds, generated deterministically with Python's standard library."""
import math
from pathlib import Path
import random
import struct
import wave

RATE = 44100
ROOT = Path(__file__).resolve().parents[1] / 'src/main/res/raw'

def render(name, duration, tonal=False, fuller=False):
    rng = random.Random(2601026)
    samples = []
    for i in range(round(RATE * duration)):
        t = i / RATE
        attack = min(1, t / .004)
        tail = min(1, (duration-t) / .018)
        if tonal:
            second = max(0, t-.095)
            base = 440 if name == "math_success" else 523.25
            a = math.sin(2*math.pi*base*t)*math.exp(-t*19)
            b = 0 if t < .095 else math.sin(2*math.pi*(base*1.25)*second)*math.exp(-second*24)*min(1,second/.008)
            value = .20*(a+b)*attack*tail
        else:
            decay = math.exp(-t*(38 if fuller else 57))
            body = math.sin(2*math.pi*(370 if fuller else 520)*t)
            overtone = .3*math.sin(2*math.pi*1080*t)
            value = .30*(body+overtone+.16*rng.uniform(-1,1))*decay*attack*tail
        samples.append(round(max(-.8,min(.8,value))*32767))
    with wave.open(str(ROOT / (name+'.wav')), 'wb') as out:
        out.setparams((1,2,RATE,0,'NONE','not compressed'))
        out.writeframes(struct.pack('<'+'h'*len(samples),*samples))
    assert max(abs(s) for s in samples) < 32767

ROOT.mkdir(parents=True,exist_ok=True)
render('wood_move',.105)
render('wood_capture',.145,fuller=True)
render('gentle_check',.235,tonal=True)
render('math_success',.265,tonal=True)
files = list(ROOT.glob('*.wav'))
assert sum(p.stat().st_size for p in files) < 120_000
for path in files:
    with wave.open(str(path),'rb') as clip:
        assert clip.getnchannels()==1 and clip.getsampwidth()==2 and clip.getframerate()==RATE
        print(path.name, path.stat().st_size, clip.getnframes()/RATE)
