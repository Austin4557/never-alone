from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter
import math, random, struct, wave, subprocess

ROOT = Path("src/main/resources/assets/never_alone")
TEX = ROOT / "textures/gui"
SND = ROOT / "sounds"
TEX.mkdir(parents=True, exist_ok=True)
SND.mkdir(parents=True, exist_ok=True)

def make_face(index, seed):
    rng = random.Random(seed)
    w, h = 1024, 576
    # Uneven near-black background with film-like grain.
    pix = []
    for y in range(h):
        vignette_y = abs(y - h/2) / (h/2)
        for x in range(w):
            vignette_x = abs(x - w/2) / (w/2)
            v = max(vignette_x, vignette_y)
            base = int(max(0, 15 - 11*v + rng.gauss(0, 3)))
            pix.append((base, base, base))
    img = Image.new("RGB", (w, h))
    img.putdata(pix)

    # Soft pale head, intentionally asymmetric and uncanny.
    layer = Image.new("L", (w, h), 0)
    d = ImageDraw.Draw(layer)
    cx = w//2 + rng.randint(-75, 75)
    cy = h//2 + rng.randint(-15, 25)
    fw = rng.randint(260, 350)
    fh = rng.randint(410, 520)
    d.ellipse((cx-fw//2, cy-fh//2, cx+fw//2, cy+fh//2), fill=rng.randint(105, 145))
    layer = layer.filter(ImageFilter.GaussianBlur(30))
    pale = Image.new("RGB", (w, h), (150+rng.randint(-10,10), 145, 140))
    img = Image.composite(pale, img, layer)

    draw = ImageDraw.Draw(img)
    eye_y = cy - rng.randint(45, 80)
    gap = rng.randint(70, 105)
    # Deep eye sockets and tiny catchlights.
    for ex in (cx-gap, cx+gap+rng.randint(-12,12)):
        ew, eh = rng.randint(55,85), rng.randint(35,60)
        draw.ellipse((ex-ew, eye_y-eh, ex+ew, eye_y+eh), fill=(5,5,5))
        px = ex + rng.randint(-18,18)
        py = eye_y + rng.randint(-8,8)
        r = rng.randint(4,8)
        draw.ellipse((px-r, py-r, px+r, py+r), fill=(205,205,195))

    # Narrow nose shadow and unsettling mouth; no gore.
    draw.polygon([(cx-18, cy-25),(cx+15,cy-25),(cx+28,cy+80),(cx-8,cy+65)], fill=(45,42,40))
    my = cy + rng.randint(105,145)
    mw = rng.randint(95,145)
    mh = rng.randint(10,32)
    draw.ellipse((cx-mw, my-mh, cx+mw, my+mh), fill=(8,8,8))
    if index % 2 == 0:
        draw.line((cx-mw+15,my,cx+mw-15,my), fill=(170,165,155), width=3)

    # Scratches/noise streaks create analog-camera unease, not injury.
    for _ in range(70):
        x = rng.randrange(w); y = rng.randrange(h)
        length = rng.randint(8,80)
        shade = rng.randint(15,45)
        draw.line((x,y,min(w-1,x+length),y+rng.randint(-2,2)), fill=(shade,shade,shade), width=1)

    img = img.filter(ImageFilter.GaussianBlur(rng.uniform(0.4,1.2)))
    # Strong vignette.
    vig = Image.new("L",(w,h),0); vd=ImageDraw.Draw(vig)
    vd.ellipse((-100,-170,w+100,h+170),fill=220)
    vig=vig.filter(ImageFilter.GaussianBlur(95))
    black=Image.new("RGB",(w,h),(0,0,0))
    img=Image.composite(img,black,vig)
    img.save(TEX / f"scare_{index}.png", optimize=True)

def make_sound(index, seed):
    rng = random.Random(seed)
    rate = 44100
    duration = 0.72 + index * 0.08
    n = int(rate * duration)
    wav_path = SND / f"scare_{index}.wav"
    with wave.open(str(wav_path), "wb") as wf:
        wf.setnchannels(1); wf.setsampwidth(2); wf.setframerate(rate)
        frames = bytearray()
        f1 = 55 + index*19
        f2 = 730 + index*117
        for i in range(n):
            t = i/rate
            env = min(1.0, t*45) * max(0.0, 1.0-t/duration)
            noise = rng.uniform(-1,1) * 0.48
            tone = math.sin(2*math.pi*f1*t)*0.32 + math.sin(2*math.pi*f2*t)*0.18
            pulse = math.sin(2*math.pi*(8+index)*t)*0.10
            sample = max(-1,min(1,(noise+tone+pulse)*env))
            frames += struct.pack("<h", int(sample*30000))
        wf.writeframes(frames)
    ogg = SND / f"scare_{index}.ogg"
    subprocess.run(["ffmpeg","-y","-loglevel","error","-i",str(wav_path),"-c:a","libvorbis","-q:a","4",str(ogg)],check=True)
    wav_path.unlink()

for i, seed in enumerate((1701, 2819, 3947, 5099), start=1):
    make_face(i, seed)
    make_sound(i, seed)

print("Generated Never Alone scare textures and sounds.")
