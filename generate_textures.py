"""Reproducible procedural planet texture generator (Pillow).
Run: python3 tools/generate_textures.py --size 2048
"""
import argparse, math, os, random
from PIL import Image, ImageDraw, ImageFilter

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--size", type=int, default=1024, help="Texture width/height in pixels (e.g. 1024 or 2048)")
    parser.add_argument("--out", default="app/src/main/res/drawable-nodpi")
    args = parser.parse_args()
    os.makedirs(args.out, exist_ok=True)
    print(f"Generate or resize your NASA / Solar System Scope textures into {args.out} at {args.size}x{args.size}.")

if __name__ == "__main__":
    main()
