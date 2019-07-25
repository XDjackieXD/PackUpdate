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
        if p[0].isdigit():
            break
        parts.append(p)
    return "-".join(parts)

def generate_mod(mod_file, url_base, flags, writer):
    zip = zipfile.ZipFile(mod_file)
    name = None
    version = None
    if 'mcmod.info' in zip.namelist():
        try:
            f = zip.open('mcmod.info')
            data = json.load(f)
            if 'modListVersion' in data and data['modListVersion'] == 2:
                data = data['modList']
            name = data[0]['name']
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
        version = ''
    our_flags = flags[name] if name in flags else ''
    writer.write("{},{},{}/mods/{},mod,{},{}\n".format(name, version, url_base, urllib.parse.quote(path.basename(mod_file)), sha256(mod_file), our_flags))

def make_configs(url_base, writer):
    with zipfile.ZipFile('configs.zip', 'w') as zip:
        for (dirname, dirs, files) in os.walk("config"):
            for dir in dirs:
                filename = path.join(dirname, dir)
                arcname = filename[7:]
                zip.write(filename, arcname)
            for file in files:
                filename = path.join(dirname, file)
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

with open(out_file, 'w') as out_file:
    make_configs(base_url, out_file)
    if path.isfile('resources.packupdate'):
        with open('resources.packupdate') as file:
            make_resources(file.readlines(), base_url, out_file)
    flags = {}
    if path.isfile('flags.packupdate'):
        with open('flags.packupdate') as file:
            for line in file.readlines():
                key, val = line.split(',')
                flags[key] = val.rstrip()
    modpath = 'mods/'
    for f in os.listdir(modpath):
        mod_file = os.path.join(modpath, f)
        if os.path.isfile(mod_file):
            generate_mod(mod_file, base_url, flags, out_file)
