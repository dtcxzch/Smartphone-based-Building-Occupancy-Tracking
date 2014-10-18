from Tkinter import *
import Image, ImageTk
from collections import deque
import csv
import sys

sys.setrecursionlimit(1000000)

root = Tk()
root.geometry("+%d+%d" % (100, 100))
root.title("Phillips Hall 3rd Floor")

floor_picture = "PH3.jpg"
floor_image = Image.open(floor_picture)

root.geometry("%dx%d" % (floor_image.size[0],floor_image.size[1]))

tkpi = ImageTk.PhotoImage(floor_image)
floor_label_image = Label(root, image=tkpi)
floor_label_image.place(x=0,y=0,width=floor_image.size[0],height=floor_image.size[1])

old_phone_label_image = None
phone_picture = "galaxyicon.png"
phone_image = Image.open(phone_picture)

def get_last_row(csv_filename):
    with open(csv_filename, 'rb') as f:
        last_row = deque(csv.reader(f), 1)[0]
        locations = last_row[2][1:-1].split(",")
        floor_number = float(last_row[3])
        return ([float(locations[0].strip()), float(locations[1].strip())], floor_number)

def draw():
    global old_phone_label_image
    
    try:
        (location, floor_number) = get_last_row("documentation.csv")
        if floor_number != 3 or (len(location) != 2):
            if old_phone_label_image is not None:
                old_phone_label_image.destroy()
                
        else:
            x = (floor_image.size[0] / 2550.0) * float(location[0])
            y = (floor_image.size[1] / 3509.0) * float(location[1])
            
            tkp_phone = ImageTk.PhotoImage(phone_image)
            phone_label_image = Label(root, image=tkp_phone)
            phone_label_image.place(x=x,y=y,width=phone_image.size[0], height=phone_image.size[1])

            if old_phone_label_image is not None:
                old_phone_label_image.destroy()

            old_phone_label_image = phone_label_image

    except:
        return

    finally:
        root.after(500, draw)
        root.mainloop()

root.after(500, draw)
root.mainloop()








    
