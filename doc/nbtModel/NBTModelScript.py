bl_info = {
    "name": "Export NBT Model",
    "category": "File",
}

import bpy
import os
import struct

def cacheIdx(cache, value):
    try:
        return cache.index(value)
    except ValueError:
        cache.append(value)
        return len(cache) - 1

def writeString(fd, string):
    os.write(fd, struct.pack('>h', len(string)))
    os.write(fd, string.encode())
    
def writeTag(fd, tp, name):
    os.write(fd, struct.pack('>bh', tp, len(name)))
    os.write(fd, name.encode())

def writeEnd(fd):
    os.write(fd, struct.pack('>b', 0))

def writeColor(fd, c):
    os.write(fd, struct.pack('>4B', 0xff, int(c.b * 255.0), int(c.g * 255.0), int(c.r * 255.0)))

def writeObject(fd, obj, images):
    if obj.type != 'MESH': return
    print("- writing sub object %s :" % obj.name)
    mesh = obj.data
    textures = mesh.uv_texture_stencil.data
    uvs = mesh.uv_layer_stencil.data
    if len(mesh.vertex_colors) > 0:
        colors = mesh.vertex_colors[0]
        if colors.name != 'all':
            hasColor = -2
            ipq = 4
        else:
            hasColor = -3
            ipq = 7
        colors = colors.data
    else:
        colors = None
        hasColor = -1
        ipq = 3
    uvlist = []
    cullfaces = []
    #tag start
    writeTag(fd, 10, obj.name)
    #color mode
    writeTag(fd, 1, "hasColor")
    os.write(fd, struct.pack('>b', hasColor))
    #vertices
    l = len(mesh.vertices)
    print("-- writing %d vertices" % l)
    writeTag(fd, 11, "vertices")
    os.write(fd, struct.pack('>i', l * 3))
    for vert in mesh.vertices.values():
        vec = vert.co
        os.write(fd, struct.pack('>3f', vec.x, vec.y, vec.z))
    #polygons
    l = len(mesh.polygons)
    print("-- writing %d polygons" % l)
    writeTag(fd, 11, "quads")
    os.write(fd, struct.pack('>i', l * ipq))
    n = 0
    for pol in mesh.polygons.values():
        tex = textures[pol.index].image
        tidx = cacheIdx(images, tex)
        v = pol.vertices
        v0 = v[0]; v1 = v[1]; v2 = v[2]
        uv0 = cacheIdx(uvlist, uvs[n].uv)
        uv1 = cacheIdx(uvlist, uvs[n+1].uv)
        uv2 = cacheIdx(uvlist, uvs[n+2].uv)
        if len(v) < 4:
            v3 = v2
            uv3 = uv2
        else:
            v3 = v[3]
            uv3 = cacheIdx(uvlist, uvs[n+3].uv)
        norm = pol.normal
        os.write(fd, struct.pack('>4B4B4b', v3, v2, v1, v0, uv3, uv2, uv1, uv0, tidx, int(norm.z * 127.0), int(norm.y * 127.0), int(norm.x * 127.0)))
        #color
        if ipq > 3:
            writeColor(fd, colors[n].color)
            if ipq > 4:
                writeColor(fd, colors[n+1].color)
                writeColor(fd, colors[n+2].color)
                if len(v) < 4:
                    writeColor(fd, colors[n+2].color)
                else:
                    writeColor(fd, colors[n+3].color)
        n += len(v)
        #get cullface
        v0 = mesh.vertices[v0].co
        v1 = mesh.vertices[v1].co
        v2 = mesh.vertices[v2].co
        v3 = mesh.vertices[v3].co
        mx = 0.9375; mn = 0.0625
        if v0.y < mn and v1.y < mn and v2.y < mn and v3.y < mn: cf = 0
        elif v0.y > mx and v1.y > mx and v2.y > mx and v3.y > mx: cf = 1
        elif v0.z < mn and v1.z < mn and v2.z < mn and v3.z < mn: cf = 2
        elif v0.z > mx and v1.z > mx and v2.z > mx and v3.z > mx: cf = 3
        elif v0.x < mn and v1.x < mn and v2.x < mn and v3.x < mn: cf = 4
        elif v0.x > mx and v1.x > mx and v2.x > mx and v3.x > mx: cf = 5
        else: cf = -1
        cullfaces.append(cf)
    #cullfaces
    print("-- writing cullfaces")
    writeTag(fd, 7, "cullfaces")
    os.write(fd, struct.pack('>i', len(cullfaces)))
    for cf in cullfaces:
        os.write(fd, struct.pack('>b', cf))
    #uvs
    l = len(uvlist)
    print("-- writing %d uv coords" % l)
    writeTag(fd, 11, "uvs")
    os.write(fd, struct.pack('>i', l * 2))
    for uv in uvlist:
        os.write(fd, struct.pack('>2f', uv.x, uv.y))
    print("- done")
    writeEnd(fd)

def save_models(context, filepath):
    print("saving models...")
    fd = os.open(filepath, os.O_CREAT | os.O_RDWR)
    objs = bpy.data.objects
    writeTag(fd, 10, "")
    images = []
    for obj in objs:
        writeObject(fd, obj, images)
    l = len(images)
    print("- writing %d texture paths" % l)
    writeTag(fd, 9, "textures")
    os.write(fd, struct.pack('>bi', 8, l))
    for image in images:
        path = image.filepath
        i = path.rfind("assets")
        if i >= 0:
            path = path[i+7:]
        i = path.find("textures")
        if i >= 0:
            path = path[0:i-1] + ':' + path[i+9:]
        if path.endswith(".png"):
            path = path[0:-4]
        writeString(fd, path)
    print("- done")
    writeEnd(fd)
    os.close(fd)
    return {'FINISHED'}


# ExportHelper is a helper class, defines filename and
# invoke() function which calls the file selector.
from bpy_extras.io_utils import ExportHelper
from bpy.props import StringProperty, BoolProperty, EnumProperty
from bpy.types import Operator

class NBTModelExport(Operator, ExportHelper):
    """Export NBT Model"""
    bl_idname = "export_scene.nbt_model"
    bl_label = "export as NBT Model"
    bl_options = {'REGISTER'}

    filename_ext = ".nbt"

    filter_glob = StringProperty(
            default="*.nbt",
            options={'HIDDEN'},
            )

    def execute(self, context):
        return save_models(context, self.filepath)


def menu_func_export(self, context):
    self.layout.operator(NBTModelExport.bl_idname, text="export as NBT-Model")


def register():
    bpy.utils.register_class(NBTModelExport)
    bpy.types.INFO_MT_file_export.append(menu_func_export)


def unregister():
    bpy.utils.unregister_class(NBTModelExport)
    bpy.types.INFO_MT_file_export.remove(menu_func_export)


if __name__ == "__main__":
    register()

