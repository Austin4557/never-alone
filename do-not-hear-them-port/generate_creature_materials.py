"""Generate original opaque model material textures and blockstate assets for DNHT.
No replacement of Minecraft's vanilla blocks or resource packs.
"""
from PIL import Image, ImageDraw
from pathlib import Path
import random, math, json

root=Path("src/main/resources/assets/donothearthemprototype")
tex=root/"textures"/"block"
models=root/"models"/"block"
states=root/"blockstates"
for dest in (tex,models,states): dest.mkdir(parents=True,exist_ok=True)
N=128
palette={
    "cave":[(12,15,20),(29,29,33),(201,181,109),(218,203,142),(218,203,142),(218,203,142)],
    "forest":[(9,14,12),(28,33,25),(105,109,80),(204,194,161),(204,194,161),(204,194,161)],
    "window":[(14,13,20),(40,36,48),(175,173,170),(210,211,205),(210,211,205),(210,211,205)],
    "sleep":[(14,13,24),(40,38,54),(108,103,137),(187,184,194),(187,184,194),(187,184,194)]
}

def field(rng):
    parts=[]
    for res,amp in [(8,13),(20,7),(43,3)]:
        tiny=Image.new("L",(res,res))
        tiny.putdata([rng.randrange(256) for _ in range(res*res)])
        parts.append((tiny.resize((N,N),Image.Resampling.BICUBIC).load(),amp))
    return parts

def base_image(rgb,rng,kind):
    im=Image.new("RGB",(N,N));dst=im.load();f=field(rng)
    for y in range(N):
        for x in range(N):
            noise=sum((src[x,y]-128)*scale/128 for src,scale in f)
            wave=3.7*math.sin(x*.087+y*.033)+2.6*math.cos(x*.15-y*.049)
            if kind=="forest":wave+=2.6*math.sin(y*.065+x*.11)
            if kind=="sleep":wave+=3*math.sin(x*.042+y*.18)
            v=noise+wave
            dst[x,y]=tuple(max(0,min(255,round(a+v))) for a in rgb)
    return im

def marks(overlay,rng,kind,variant):
    d=ImageDraw.Draw(overlay,"RGBA")
    if kind=="forest":
        # Discontinuous irregular bark grain; avoids v14's straight repeated stripes.
        for i in range(31 if variant!=2 else 16):
            x=rng.uniform(-14,142);y=rng.uniform(-30,85)
            length=rng.uniform(12,50); segments=rng.randint(4,9)
            slope=rng.uniform(-.13,.13)
            points=[(x+slope*(length*j/segments)
                    +2.5*math.sin((y+length*j/segments)*.07+i*.67)
                    +rng.uniform(-1.4,1.4),y+length*j/segments)
                    for j in range(segments+1)]
            d.line(points,fill=rng.choice([(18,17,10,55),(24,18,9,68),
                (8,11,5,37),(109,81,48,48)]),width=rng.choice([1,2,2,3]))
            if variant==1 and rng.random()<.6:
                d.line([(a+2,b) for a,b in points],fill=(139,110,67,33),width=1)
        if variant==2:
            for i in range(45):
                x,y=rng.randrange(N),rng.randrange(N)
                w,h=rng.randrange(2,9),rng.randrange(1,7)
                d.ellipse((x-w,y-h,x+w,y+h),fill=(52,103,61,rng.randrange(27,85)))
        for i in range(16):
            x,y=rng.randrange(N),rng.randrange(N)
            d.ellipse((x,y,x+2,y+3),fill=(170,138,83,rng.randrange(16,49)))
    elif kind=="cave":
        for i in range(24):
            x,y=rng.randrange(-16,144),rng.randrange(-16,144)
            span=rng.randrange(6,26);dx=rng.choice([-1,1])*rng.randrange(5,15)
            pts=[(x,y),(x+span//2,y+rng.randrange(-4,6)),
                 (x+span+dx,y+rng.randrange(6,21))]
            d.line(pts,fill=(6,12,18,rng.randrange(28,84)),width=rng.choice([1,2,3]))
            d.line([(a+2,b-1) for a,b in pts],fill=(145,145,158,rng.randrange(22,62)),width=1)
        for i in range(15):
            x,y=rng.randrange(N),rng.randrange(N)
            w,h=rng.randrange(3,15),rng.randrange(4,20)
            d.polygon([(x,y),(x+w,y+2),(x+w//2,y+h)],fill=(6,13,20,28))
        if variant==2:
            for i in range(13):
                x,y=rng.randrange(N),rng.randrange(N)
                d.line((x,y,x+4,y+1),fill=(75,132,139,75),width=1)
    elif kind=="window":
        if variant==2:
            for i in range(48):
                x,y=rng.randrange(N),rng.randrange(N);r=rng.randrange(1,6)
                d.ellipse((x-r,y-r,x+r,y+r),fill=(40,38,48,rng.randrange(12,56)))
            for i in range(17):
                x,y=rng.randrange(N),rng.randrange(N)
                d.line((x,y,x+rng.randrange(-10,10),y+rng.randrange(6,17)),
                       fill=(67,62,66,75),width=1)
        else:
            for i in range(30):
                x,y=rng.randrange(-8,136),rng.randrange(N);length=rng.randrange(13,46)
                d.line((x,y,x+rng.randrange(-8,9),y+length),
                       fill=(124,114,123,rng.randrange(17,44)),width=rng.randrange(1,4))
            for i in range(30):
                x,y=rng.randrange(N),rng.randrange(N)
                d.line((x,y,x+5,y+5),fill=(8,9,14,55),width=1)
    else:
        for i in range(32):
            x,y=rng.randrange(-10,140),rng.randrange(-10,140)
            drift=rng.uniform(-15,16);length=rng.uniform(10,40)
            pts=[(x+drift*math.sin(j/9*math.pi),y+length*j/9) for j in range(10)]
            shade=(148,121,184,rng.randrange(20,65)) if i%3 else (5,4,17,rng.randrange(22,67))
            d.line(pts,fill=shade,width=rng.randrange(1,4))
        for i in range(10):
            x,y=rng.randrange(N),rng.randrange(N)
            d.arc((x-8,y-6,x+25,y+24),rng.randrange(90),rng.randrange(150,270),
                  fill=(146,117,185,30),width=2)


def face_texture(kind,base,blink=0):
    """Original *new* illustration for the face surface, not extracted mod artwork."""
    image=Image.new("RGB",(N,N),base)
    d=ImageDraw.Draw(image)
    if kind=="cave":
        # Cyclopean sun-eye, with the rays coming from 3D geometry.
        d.ellipse((18,27,110,101),fill=(51,42,23))
        d.ellipse((26,34,102,93),fill=(239,223,170))
        d.ellipse((43,34,86,94),fill=(172,144,91))
        d.ellipse((55,37,78,92),fill=(17,20,21))
        d.ellipse((61,43,69,59),fill=(249,245,212))
        for x in (24,100):
            d.arc((x-10,17,x+10,111),70,285,fill=(246,228,165),width=3)
    elif kind=="forest":
        # Narrow, pale wooden skull-mask. Retain dark forest creature around it.
        d.polygon([(33,13),(89,13),(104,45),(94,103),(64,118),
                   (31,98),(22,43)],fill=(210,203,173))
        d.ellipse((31,42,55,76),fill=(12,17,13))
        d.ellipse((72,42,96,76),fill=(12,17,13))
        d.ellipse((41,55,47,64),fill=(230,173,68))
        d.ellipse((82,55,88,64),fill=(230,173,68))
        d.polygon([(63,73),(55,91),(68,93)],fill=(35,36,27))
        for x in (47,59,71,83):
            d.line((x,97,x-2,106),fill=(60,58,45),width=2)
    elif kind=="window":
        # Oversized smooth watcher mask, hollow eye sockets, unnerving smile.
        d.ellipse((13,6,115,124),fill=(223,224,216))
        d.ellipse((24,26,58,75),fill=(12,11,16))
        d.ellipse((70,26,104,75),fill=(12,11,16))
        d.ellipse((34,32,43,47),fill=(241,241,226))
        d.ellipse((82,32,91,47),fill=(241,241,226))
        d.polygon([(64,65),(55,84),(71,83)],fill=(88,83,87))
        d.arc((31,64,97,113),6,174,fill=(18,15,22),width=9)
        for x in range(42,91,11):
            d.line((x,91,x+1,101),fill=(231,227,211),width=2)
    else:
        # Pale, almost-featureless sleep apparition with deep eyes.
        d.ellipse((17,8,111,120),fill=(197,194,209))
        d.ellipse((28,36,55,85),fill=(12,11,21))
        d.ellipse((72,36,99,85),fill=(12,11,21))
        d.ellipse((38,52,45,64),fill=(173,168,214))
        d.ellipse((82,52,89,64),fill=(173,168,214))
        d.line((58,98,72,98),fill=(37,32,48),width=3)

    # Render fine facial surface details at 256px, leaving the original
    # silhouettes recognizable while avoiding the low-resolution mask look.
    image=image.resize((256,256),Image.Resampling.LANCZOS)
    d=ImageDraw.Draw(image)
    rng=random.Random(8124+sum(map(ord,kind)))
    if kind=="cave":
        # Layered iris, uneven light and subtle cracks around the cyclopean eye.
        for radius,color in [(49,(66,56,32)),(42,(133,118,74)),(31,(204,184,118))]:
            cx,cy=128,128
            d.arc((cx-radius*2,cy-radius,cx+radius*2,cy+radius),rng.randrange(70),rng.randrange(200,315),fill=color,width=2)
        d.ellipse((122,92,137,111),fill=(245,238,206))
        for i in range(18):
            a=(i/18)*math.pi*2
            rad=rng.randrange(33,53)
            x=128+math.cos(a)*rad*1.22;y=128+math.sin(a)*rad
            d.line((x,y,x+math.cos(a)*8,y+math.sin(a)*5),
                   fill=(99,76,46),width=1)
        for pts in [[(31,81),(43,71),(56,74)],[(202,176),(215,184),(225,181)],[(37,189),(53,184),(60,192)]]:
            d.line(pts,fill=(99,76,53),width=2)
    elif kind=="forest":
        # Bark-mask grain, fine angular cheekbones and small amber catchlights.
        for i in range(33):
            x=rng.randrange(62,199);y=rng.randrange(26,226)
            length=rng.randrange(6,26)
            d.line((x,y,x+rng.randrange(-7,8),y+length),
                   fill=rng.choice([(96,89,67),(131,124,90),(166,155,116)]),width=1)
        d.arc((61,74,122,162),45,155,fill=(58,64,45),width=3)
        d.arc((140,75,202,162),20,145,fill=(58,64,45),width=3)
        for p in [(88,118),(169,118)]:
            d.ellipse((p[0],p[1],p[0]+6,p[1]+9),fill=(255,210,115))
        d.line(((124,169),(109,192),(133,194)),fill=(64,57,45),width=2)
    elif kind=="window":
        # Smooth pale face, faint eye-bag shading and a less perfectly even smile.
        d.arc((49,89,112,180),10,158,fill=(143,145,146),width=3)
        d.arc((142,85,214,180),18,150,fill=(143,145,146),width=3)
        d.arc((40,22,220,239),192,353,fill=(165,164,157),width=3)
        d.arc((84,184,186,223),5,170,fill=(81,67,71),width=2)
        d.line(((127,124),(118,165),(141,170)),fill=(122,114,116),width=2)
        for x,y in [(74,80),(172,82)]:
            d.ellipse((x,y,x+11,y+11),fill=(246,244,226))
        for x,y in [(63,165),(192,169),(88,198)]:
            d.line((x,y,x+8,y+5),fill=(171,162,166),width=1)
    else:
        # Apparition: soft ghostly edge highlights and uneven haunted eyes.
        d.arc((29,21,225,238),190,340,fill=(236,231,248),width=3)
        d.arc((45,39,209,221),25,148,fill=(148,145,174),width=2)
        d.line(((110,172),(122,180),(144,174)),fill=(150,147,169),width=2)
        for x,y in [(72,112),(166,117)]:
            d.ellipse((x,y,x+9,y+13),fill=(225,219,254))
        for i in range(9):
            x=rng.randrange(48,208)
            y=rng.randrange(165,228)
            d.line((x,y,x+rng.randrange(-8,9),y+rng.randrange(4,14)),
                   fill=(164,158,187),width=1)
    # Preserve the full mask and move only the lids. These are truly different
    # face textures, switched by server-side head block-display state.
    if blink:
        sockets = {
            "cave":[(69,79,191,175)],
            "forest":[(62,84,110,151),(144,84,192,151)],
            "window":[(48,51,117,150),(139,51,209,150)],
            "sleep":[(56,70,111,171),(144,70,198,171)]
        }
        skin = {
            "cave":(165,144,96),
            "forest":(187,177,146),
            "window":(220,221,214),
            "sleep":(190,186,201)
        }[kind]
        crease = {
            "cave":(80,61,40),"forest":(69,64,50),
            "window":(95,88,91),"sleep":(104,98,123)
        }[kind]
        for x0,y0,x1,y1 in sockets[kind]:
            height=y1-y0
            if blink==1:  # half-lidded
                lid_bottom=y0+round(height*.68)
                d.rounded_rectangle((x0,y0,x1,lid_bottom),radius=5,fill=skin)
                d.line((x0+5,lid_bottom,x1-5,lid_bottom+2),fill=crease,width=3)
            else: # eyes shut
                d.rounded_rectangle((x0,y0,x1,y1),radius=6,fill=skin)
                yy=y0+round(height*.60)
                d.arc((x0+1,yy-7,x1-1,yy+11),2,176,fill=crease,width=4)
        if kind=="cave" and blink==2:
            d.arc((61,70,194,194),15,175,fill=(111,86,52),width=3)
    return image


for kind,colors in palette.items():
    for variant,rgb in enumerate(colors):
        rng=random.Random(1601+sum(map(ord,kind))*19+variant*91)
        image=base_image(rgb,rng,kind).convert("RGBA")
        layer=Image.new("RGBA",(N,N),(0,0,0,0))
        marks(layer,rng,kind,variant)
        image=Image.alpha_composite(image,layer).convert("RGB")
        # Real 256px custom body materials: subtle high-frequency skin grain
        # adds definition while preserving the original dark silhouette.
        image=image.resize((256,256),Image.Resampling.LANCZOS)
        name=f"skin_{kind}_{variant}"
        image.save(tex/(name+".png"),optimize=True)
        if variant>=3:
            front_name=name+"_front"
            face_texture(kind,rgb,0 if variant==3 else 1 if variant==4 else 2).save(
                tex/(front_name+".png"),optimize=True)
            side=f"donothearthemprototype:block/{name}"
            front=f"donothearthemprototype:block/{front_name}"
            (models/(name+".json")).write_text(json.dumps({
                "parent":"minecraft:block/cube",
                "textures":{"down":side,"up":side,"east":side,"west":side,
                            "north":side,"south":front,"particle":side},
                "ambientocclusion":False
            }))
        else:
            (models/(name+".json")).write_text(json.dumps({
                "parent":"minecraft:block/cube_all",
                "textures":{"all":f"donothearthemprototype:block/{name}"},
                "ambientocclusion":False
            }))
        (states/(name+".json")).write_text(json.dumps({
            "variants":{"":{"model":f"donothearthemprototype:block/{name}"}}
        }))
        print("Generated opaque 256x256 3D material:",name)
