#!/usr/bin/env python3

import json
import os
from os import path
import sys
import zipfile
import hashlib
import urllib.parse

BLOCKSIZE = 65536

def sha256(file):
    hasher = hashlib.sha256()
    with open(file, 'rb') as afile:
        buf = afile.read(BLOCKSIZE)
        while len(buf) > 0:
            hasher.update(buf)
            buf = afile.read(BLOCKSIZE)
    return(hasher.hexdigest())

def guess_mod_name(file_name):
    file_name, _ = os.path.splitext(file_name)
    parts = []
    for p in file_name.split('-'):
        if len(p) > 0 and p[0].isdigit():
            break
        parts.append(p)
    return "-".join(parts)

def apply_mod_count(modcount, modid):
    if modid in modcount:
        count = modcount[modid]
        modcount[modid] = count + 1
        return "{}-{}".format(modid, count)
    else:
        modcount[modid] = 1
        return modid

def generate_mod(mod_file, url_base, flags, writer, modcount):
    zip = zipfile.ZipFile(mod_file)
    mod_sha = sha256(mod_file)
    name = None
    version = None
    if 'mcmod.info' in zip.namelist():
        try:
            f = zip.open('mcmod.info')
            data = json.load(f)
            if 'modListVersion' in data and data['modListVersion'] == 2:
                data = data['modList']
            name = data[0]['modid']
            if 'version' in data[0]:
                version = data[0]['version']
            else:
                print("Warning: Mod {} is apparently incapable of specifying a version number in their mcmod.info. Using 'unknown', this may have weird side effects".format(name))
                version = 'unknown'
        except ValueError as e:
            print("Warning: Mod {} does not contain mcmod.info (or it does not follow correct format). Guessing information, this may have weird side effects".format(mod_file))
        except json.decoder.JSONDecodeError as e:
            print("Warning: Author of mod {} is apparently incapable of writing correctly formatted json. Guessing information, this may have weird side effects ({})".format(mod_file, e))
        except Exception as e:
            print("Irgendwas kaputt: {}".format(e))
    else:
        print("Warning: Mod {} does not contain mcmod.info (or it does not follow correct format). Guessing information, this may have weird side effects".format(mod_file))
    if name == None:
        name = guess_mod_name(path.basename(mod_file))
    
    if version == None or not version[0].is_digit():
        # Default the version to the first 8 bytes of the sha of it's unknown, or doesn't look version-like (digits)
        # This makes it so that the update logic isn't getting stuck on mod updates where the version hasn't changed.
        version = mod_sha[0:8]
    name = apply_mod_count(modcount, name)
    our_flags = flags[name] if name in flags else ''
    writer.write("{},{},{}/mods/{},mod,{},{}\n".format(name, version, url_base, urllib.parse.quote(path.basename(mod_file)), mod_sha, our_flags))

def make_configs(url_base, writer, exclude):
    """
    Creates a configs.zip from the config/ directory.

    Can be given a list of filenames to exclude
    """
    with zipfile.ZipFile('configs.zip', 'w') as zip:
        for (dirname, dirs, files) in os.walk("config"):
            if dirname in exclude:
                print("Skipping " + dirname + " and all files in it")
                continue

            for dir in dirs:
                filename = path.join(dirname, dir)
                arcname = filename[7:]
                if filename not in exclude:
                    zip.write(filename, arcname)

            for file in files:
                filename = path.join(dirname, file)
                if filename in exclude:
                    print("Skipping " + filename)
                    continue
                arcname = filename[7:]
                zip.write(filename, arcname)

    writer.write("Configs,{1},{0}/configs.zip,config,{1}\n".format(url_base, sha256('configs.zip')))

def path_to_tree(path):
    ret = set([])
    total_path = ""
    for el in path.split("/"):
        total_path += el + "/"
        ret.add(total_path)
    return ret

def make_resources(list, url_base, writer):
    dirs = set([])
    for p in list:
        dirname = path.dirname(p)
        if len(dirname) > 0:
            dirs = dirs.union(path_to_tree(dirname))
    with zipfile.ZipFile('resources.zip', 'w') as zip:
        for dir in dirs:
            zip.write(dir, dir)
        for file in list:
            file = file.rstrip()
            zip.write(file, file)
    writer.write("Resources,{1},{0}/resources.zip,resources,{1}\n".format(url_base, sha256('resources.zip')))

if len(sys.argv) != 3:
    print("Usage: {} <url_base> <out_file>".format(sys.argv[0]))
    sys.exit(1)

base_url = sys.argv[1]
out_file = sys.argv[2]

exclude = []
if path.isfile('exclude.packupdate'):
    with open('exclude.packupdate') as file:
        for line in file.readlines():
            exclude.append(line.strip())

with open(out_file, 'w') as out_file:
    make_configs(base_url, out_file, exclude)
    if path.isfile('resources.packupdate'):
        with open('resources.packupdate') as file:
            make_resources(file.readlines(), base_url, out_file)
    if path.isfile('forge.packupdate'):
        with open('forge.packupdate') as file:
            out_file.write("Minecraft Forge,{},,forge\n".format(file.read().strip()))
    flags = {}
    if path.isfile('flags.packupdate'):
        with open('flags.packupdate') as file:
            for line in file.readlines():
                key, val = line.split(',')
                flags[key] = val.rstrip()
    modpath = 'mods/'
    modcount = {}
    for f in os.listdir(modpath):
        mod_file = os.path.join(modpath, f)
        if mod_file in exclude:
            continue
        if os.path.isfile(mod_file):
            generate_mod(mod_file, base_url, flags, out_file, modcount)
