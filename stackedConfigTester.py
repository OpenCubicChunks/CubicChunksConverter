from PIL import Image, ImageDraw, ImageOps
from random import randint

class Vec2i:
	def __init__(self, x, z):
		self.x = x
		self.z = z

class BoundingBox:
	def __init__(self, minVal, maxVal):
		self.minVal = minVal
		self.maxVal = maxVal

	def addX(self, val):
		self.minVal.x += val
		self.maxVal.x += val

	def addZ(self, val):
		self.minVal.z += val
		self.maxVal.z += val

	def overlapOf(self, other): #Returns the rectangle that is the overlap of the two supplied boxes
		minX = max(self.minVal.x, other.minVal.x)
		minZ = max(self.minVal.z, other.minVal.z)

		maxX = min(self.maxVal.x, other.maxVal.x)
		maxZ = min(self.maxVal.z, other.maxVal.z)

		if (minX > maxX  or minZ > maxZ ):
			return None # no intersection
		else:
			return BoundingBox(Vec2i(minX, maxZ), Vec2i(maxX, minZ)) #intersection

	def __repr__(self):
		return "(" + str(self.minVal.x) + ", " + str(self.minVal.z) + "), (" + str(self.minVal.x) + ", " + str(self.minVal.z) + ")"
	def __str__(self):
		return self.__repr__()

	@staticmethod
	def minX(boxes):
		minimum = 0
		for box in boxes:
			if(box.minVal.x < minimum):
				minimum = box.minVal.x
		return minimum

	@staticmethod
	def maxX(boxes):
		maximum = 0
		for box in boxes:
			if(box.maxVal.x > maximum):
				maximum = box.maxVal.x
		return maximum

	@staticmethod
	def minZ(boxes):
		minimum = 0
		for box in boxes:
			if(box.minVal.z < minimum):
				minimum = box.minVal.z
		return minimum

	@staticmethod
	def maxZ(boxes):
		maximum = 0
		for box in boxes:
			if(box.maxVal.z > maximum):
				maximum = box.maxVal.z
		return maximum

def loadDataFromFile(filename):
	with open(filename) as f:
		lines = f.readlines()
		#print(lines)
		boxes = {}

		for line in lines:
			split = line.split(" ")

			if(len(split) <= 1):
				continue

			if(split[1] == "box"):
				if(split[0] not in boxes):
					boxes[split[0]] = []
				boxes[split[0]].append(BoundingBox(Vec2i(int(split[2]), int(split[4])), Vec2i(int(split[5]), int(split[7]))))

		boxesList = []
		for key in boxes:
			for l in boxes[key]:
				boxesList.append(l)

		return boxesList

boundingBoxes = loadDataFromFile("stackedConfig.txt")

minX = BoundingBox.minX(boundingBoxes)
minZ = BoundingBox.minZ(boundingBoxes)
for box in boundingBoxes:
	box.addX(abs(minX))
	box.addZ(abs(minZ))

imgScale = 1
im = Image.new("RGB", (int(BoundingBox.maxX(boundingBoxes) * imgScale), int(BoundingBox.maxZ(boundingBoxes) * imgScale)), (255, 255, 255))
draw = ImageDraw.Draw(im)

for i, box1 in enumerate(boundingBoxes): #For each box
	overlaps = False
	for j, box2 in enumerate(boundingBoxes): #For each box
		if(box1 != box2):
			b3 = box1.overlapOf(box2) #overlap of returns the rectangle that is the overlap of box a and b
			if(b3 != None): #If the boxes overlap (None == no overlap)
				overlaps = True
				print(str(i) + " overlaps " + str(j))
				draw.rectangle((b3.minVal.x * imgScale, b3.minVal.z * imgScale, b3.maxVal.x * imgScale, b3.maxVal.z * imgScale), fill=(255, 0, 0)) #draw the overlap in red
	if(overlaps == False):
		draw.rectangle((box1.minVal.x * imgScale, box1.minVal.z * imgScale, box1.maxVal.x * imgScale, box1.maxVal.z * imgScale), fill=(randint(0, 30), randint(230, 255), randint(0, 30))) #if a box has no overlap, draw it in green, otherwise the box is not drawn to clearly show the overlaps

im = ImageOps.flip(im) #flip vertically so that 0,0 is bottom left (as most people would expect)
im.save("testimg.jpg", quality=100)