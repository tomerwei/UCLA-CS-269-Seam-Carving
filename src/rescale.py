#!/bin/env python

import numpy as np
from scipy.ndimage.filters import laplace 
from PIL import Image

import sys

def cost(image):
    return laplace(image)**2

def find_seams(image):
    #print "Finding seams"
    im_w, im_h = image.shape
    seams_raw = {}
    costs = {}
    for y in range(image.shape[1]):
        costs[(0, y)] = image[1, y]
    for x in range(image.shape[0]):
        costs[(x, -1)] = np.inf
        costs[(x, image.shape[1])] = np.inf
    for x in range(1, image.shape[0]):
        for y in range(image.shape[1]):
            m = min(costs[x-1,y-1], costs[x-1,y], costs[x-1,y+1])
            costs[(x, y)] = m + image[x,y]
            if m == costs[x-1, y]:
                seams_raw[(x,y)] = (x-1, y)
            elif m == costs[x-1, y-1]:
                seams_raw[(x,y)] = (x-1, y-1)
            elif m == costs[x-1, y+1]:
                seams_raw[(x,y)] = (x-1, y+1)

    seams = {}
    i = image.shape[0]-1
    min_cost = np.inf
    for j in range(image.shape[1]):
        path = [(i,j),]
        total_cost = 0
        x, y = i, j
        try:
            while x > 0:
                total_cost += costs[x, y]
                if total_cost > min_cost:
                    raise KeyError
                x,y = seams_raw[(x,y)]
                path.append((x,y))
        except KeyError:
            continue
        total_cost += costs[x, y]
        if y in seams and total_cost > seams[y]["cost"]:
            continue
        seams[y] = {"start":j, "cost":total_cost, "path":path}
    return sorted(seams.values(), key=lambda x : x["cost"])

def remove_path(image, path):
    #print "Removing Path"
    im_w, im_h, im_c = image.shape
    image[zip(*path)] = -1
    return image[image>=0].reshape((im_w, im_h-len(path)//im_w, im_c))

def stretch_path(image, paths):
    #print "Stretching Path"
    im_w, im_h, im_c = image.shape
    image_new = np.concatenate((image, np.zeros((im_w,len(paths)//im_w, im_c))), axis=1)
    for x, y in paths:
        # This statement deals with if we have already made changes in this row
        y_orig = y
        while np.any(image[x,y_orig] != image_new[x,y]):
            y += 1
        interpolated = (np.sum(image_new[x,y-1:y+2], axis=0)+np.sum(image_new[x-1:x+2,y], axis=0))/6.
        image_new[x,:,:] = np.vstack((image_new[x,:y], interpolated.reshape((1,3)), image_new[x, y:-1]))
    return image_new

def resize(image, dim):
    #print "Resizing"
    todo = [True, True]
    while any(todo):
        if dim[1] != image.shape[1]:
            virtical_seams = find_seams(cost(image.sum(axis=-1)/3.))
            num_needed = abs(image.shape[1]-dim[1])
            #print "Found %d vert seams (%d more)"%(len(virtical_seams), num_needed)
            allpaths = reduce(lambda a,b : a+b, (seam["path"] for seam in virtical_seams[:num_needed]))
            if dim[1] < image.shape[1]:
                image = remove_path(image, allpaths)
            else:
                image = stretch_path(image, allpaths)
        else:
            todo[1] = False

        if dim[0] != image.shape[0]:
            horizontal_seams = find_seams(cost((image.sum(axis=-1)/3.).T))
            num_needed = abs(image.shape[0]-dim[0])
            #print "Found %d horiz seams (%d more)"%(len(horizontal_seams), num_needed)
            allpaths = reduce(lambda a,b : a+b, (seam["path"] for seam in horizontal_seams[:num_needed]))
            if dim[0] < image.shape[0]:
                image = remove_path(image.swapaxes(0,1), allpaths).swapaxes(0,1)
            else:
                image = stretch_path(image.swapaxes(0,1), allpaths).swapaxes(0,1)
        else:
            todo[0] = False
    return image

if __name__ == "__main__":
    filename = sys.argv[1]
    height = int(sys.argv[2])
    width = int(sys.argv[3])
    num_seg_height = int(raw_input("Number of vertically running segments?: "))
    num_seg_width = int(raw_input("Number of horizontally running segments? "))
    
    #num_seg_width = 30
    #num_seg_height = 30
    
    

     
    im = Image.open(filename)
    image = np.asarray(im, dtype=np.float)
    
    old_seg_width = image.shape[0]/num_seg_width
    old_seg_height = image.shape[1]/num_seg_height

    new_seg_width = width/num_seg_width
    new_seg_height = height/num_seg_height
    
    print "old_seg_width", old_seg_width , "old_seg_height", old_seg_height
    print "new_seg_width", new_seg_width, "new_seg_height", new_seg_height
    
    new_image = np.zeros((num_seg_width,num_seg_height, new_seg_width,new_seg_height,3))
    new_image_1 = np.zeros((width,height,3))
    for seg_width in range(0,num_seg_width):
        for seg_height in range(0,num_seg_height):
            print "width start:",seg_width*old_seg_width, "end width:",(seg_width+1)*old_seg_width
            print "height start:",seg_height*old_seg_height, "end height:", (seg_height+1)*old_seg_height
            new_image[seg_width,seg_height]= resize(image[seg_width*old_seg_width:(seg_width+1)*old_seg_width,
                                                         (seg_height)*old_seg_height:(seg_height+1)*old_seg_height],
                                                        (new_seg_width,new_seg_height));
            print new_image[seg_width,seg_height].shape
            new_image_1[seg_width*new_seg_width:(seg_width+1)*new_seg_width, (seg_height)*new_seg_height:(seg_height+1)*new_seg_height] = new_image[seg_width,seg_height]
            new_filename= ""
            new_filename = "images/output"+ str(seg_width*new_seg_width)+ "_"+ str(seg_height*new_seg_height)+ filename
            print new_filename
            Image.fromarray(new_image[seg_width,seg_height].astype('uint8')).save(new_filename)
    segmented_filename = "segmented"+str(num_seg_height)+"x"+str(num_seg_width)+"_"+str(height)+"x"+str(width)+filename
    Image.fromarray(new_image_1.astype('uint8')).save(segmented_filename)
    #file_name = filename+"_seg_cais.png"
    #resize(image, (width, height)
