#For resource pack creators this file explains the model generation script for TESR model parts.
#It works pretty much like a very simple script programming language.
#When using notepad++ import lang.xml as user defined language to get syntax highlighting.

#this line is a comment

#text
text = "this is a text";
#number
num = 5.3;
#create a vector
vec = [0, num, 0];
#combine vectors
vec2 = [vec, 3, 2, vec, num];
#access vector component (counted from zero)
val = vec2:4;

#create a quad containing 4 vertices created out of vectors with the given component order:
#absent variables are set to default (x,y,z,u,v = 0; r,g,b,a = 1).
#x y z = vertex coords, u v = texture coords, r g b a color (red green blue alpha).
quad = quad(vec2, vec2, [0, 3, 7, 8, 9, 0, val, 0], vec2, "x y z u v r g b a");

#create a quad face of the cuboid defined by first vector with given orientation & uv-mapping (must be 3 characters for "xyz"): 
#use '+' or '-' at the axis used as normal into positive/negative direction, 'u' or 'U' at the axis bound to texture U-axis and 'v' or 'V' at the axis bound to texture V-axis (upper case mirrors the texture).
quad = rect([x0, y0, z0, x1, y1, z1], [u0, v0, u1, v1], "uv+");

#run other sub models with current transformation and given variables.
mod = model("automation:models/tileEntity/example"){
	par0 = [5, 5, 5];
	par1 = vec2;
};
#access variables from sub model
vec3 = mod:par0;

#for loop starting at value of first parameter (rounded to integer), increasing counter variable (second parameter) by one each run 
#while below value of third parameter (rounded to integer and is final). changes to the counter variable don't effect loop behavior and will reset every iteration.
for(val < i < 35){
	#do some stuff here
}

#commands for color multiplier, vertex and texture transformation 
push;
pop;
rotate([0, 0, 0, 0]);
translate([0, 0, 0]);
scale([0, 0, 0]);
offsetUV([0, 0]);
scaleUV([0, 0]);
color([0, 0, 0, 0]);
#bake quad into model with current vertex, texture and color transformation applied
draw(quad);

#functions (bracket syntax is important! don't do like b = -a or c = a + b)
#sum of any amount of vectors or numbers
vec = +([0, 1], [2, 6], [9, 3]);
x = +(x, x, 3, 7, 8);
#negation (1 arg) or subtraction (2 args) of vectors or numbers
vec = -([2, 3]);
vec = -(vec, [4, 5]);
x = -(5);
x = -(x, 2);
#product of any amount of vectors or numbers
vec = *(vec, [2, 6], [9, 3]);
x = *(x, x, 3, 7, 8);
#inversion (1 arg) or division (2 args) of vectors or numbers
vec = /([2, 3]);
vec = /(vec, [4, 5]);
x = /(5);
x = /(x, 2);
#cross product of two 3-component vectors
vec = x([1, 1, 0], [0, 0, 1]);
#one vector scaled or skalar product of two vectors
vec = °(x, vec);
x = °(vec, [2, 4, 1]);
#normalized vector
vec = n([3, 4, 5]);
#length of a vector
x = l(vec);