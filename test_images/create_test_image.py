from PIL import Image, ImageDraw, ImageFont

# Create a test PNG image
img = Image.new('RGB', (200, 150), color='lightblue')
draw = ImageDraw.Draw(img)
draw.rectangle([10, 10, 190, 140], outline='darkblue', width=3)
draw.text((50, 60), 'Test Image', fill='darkblue')
img.save('test_image.png')

# Create a test JPEG image
img2 = Image.new('RGB', (200, 150), color='lightgreen')
draw2 = ImageDraw.Draw(img2)
draw2.ellipse([20, 20, 180, 130], outline='darkgreen', width=3)
draw2.text((50, 60), 'Test JPEG', fill='darkgreen')
img2.save('test_image.jpg', 'JPEG')

print("Test images created successfully")
