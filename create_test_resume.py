#!/usr/bin/env python3
"""
Simple script to create a mock resume PDF for testing
Requires: pip install reportlab
"""

try:
    from reportlab.lib.pagesizes import letter
    from reportlab.pdfgen import canvas
    from reportlab.lib.units import inch
    import os
    import sys

    def create_test_resume(filename="test_resume.pdf"):
        """Create a simple test resume PDF"""
        
        # Create PDF
        c = canvas.Canvas(filename, pagesize=letter)
        width, height = letter
        
        # Title
        c.setFont("Helvetica-Bold", 24)
        c.drawString(1*inch, height - 1*inch, "Test Student Resume")
        
        # Contact Information
        y = height - 1.5*inch
        c.setFont("Helvetica", 12)
        c.drawString(1*inch, y, "Email: test.student@example.com")
        y -= 20
        c.drawString(1*inch, y, "Phone: +1 (555) 123-4567")
        y -= 20
        c.drawString(1*inch, y, "Location: Test City, Test State")
        
        # Education Section
        y -= 40
        c.setFont("Helvetica-Bold", 16)
        c.drawString(1*inch, y, "EDUCATION")
        y -= 25
        c.setFont("Helvetica-Bold", 12)
        c.drawString(1*inch, y, "Test University")
        y -= 15
        c.setFont("Helvetica", 11)
        c.drawString(1*inch, y, "Bachelor of Science in Computer Science")
        y -= 15
        c.drawString(1*inch, y, "Expected Graduation: 2025")
        y -= 15
        c.drawString(1*inch, y, "GPA: 3.8/4.0")
        
        # Experience Section
        y -= 40
        c.setFont("Helvetica-Bold", 16)
        c.drawString(1*inch, y, "EXPERIENCE")
        y -= 25
        c.setFont("Helvetica-Bold", 12)
        c.drawString(1*inch, y, "Software Development Intern")
        y -= 15
        c.setFont("Helvetica", 11)
        c.drawString(1*inch, y, "Test Company Inc. | Summer 2024")
        y -= 15
        c.drawString(1.2*inch, y, "• Developed Android applications using Java")
        y -= 15
        c.drawString(1.2*inch, y, "• Collaborated with team on mobile app features")
        y -= 15
        c.drawString(1.2*inch, y, "• Participated in code reviews and testing")
        
        # Skills Section
        y -= 40
        c.setFont("Helvetica-Bold", 16)
        c.drawString(1*inch, y, "SKILLS")
        y -= 25
        c.setFont("Helvetica", 11)
        c.drawString(1*inch, y, "Programming Languages: Java, Kotlin, Python")
        y -= 15
        c.drawString(1*inch, y, "Mobile Development: Android SDK, Android Studio")
        y -= 15
        c.drawString(1*inch, y, "Tools: Git, Firebase, Supabase, REST APIs")
        
        # Save PDF
        c.save()
        print(f"✓ Test resume created: {filename}")
        print(f"  File size: {os.path.getsize(filename)} bytes")
        print(f"  Location: {os.path.abspath(filename)}")
        return filename

    if __name__ == "__main__":
        filename = sys.argv[1] if len(sys.argv) > 1 else "test_resume.pdf"
        create_test_resume(filename)
        print("\nNext steps:")
        print("1. Transfer this file to your Android device")
        print("2. Use ADB: adb push test_resume.pdf /sdcard/Download/")
        print("3. Or email it to yourself and download on device")

except ImportError:
    print("Error: reportlab library not found")
    print("Install it with: pip install reportlab")
    print("\nAlternatively, use one of these methods:")
    print("1. Create PDF using Google Docs or Canva")
    print("2. Use online PDF generators")
    print("3. Take a screenshot of a resume and save as PDF")

