// src/components/Footer.jsx
import React from 'react';
import { Link } from 'react-router-dom'; // If you use React Router for internal links
import { FaFacebookF, FaGithub, FaLinkedinIn, FaTwitter, FaEnvelope, FaPhoneAlt, FaMapMarkerAlt } from 'react-icons/fa';

function Footer() {
  const currentYear = new Date().getFullYear();

  const socialLinks = [
    { icon: FaFacebookF, href: 'https://facebook.com/auchub', label: 'Facebook' },
    { icon: FaGithub, href: 'https://github.com/auchub', label: 'GitHub' },
    { icon: FaTwitter, href: 'https://twitter.com/auchub', label: 'Twitter' },
    { icon: FaLinkedinIn, href: 'https://linkedin.com/company/auchub', label: 'LinkedIn' },
  ];

  return (
    <footer className="bg-slate-900 text-slate-300 border-t border-slate-700/50">
      <div className="container mx-auto px-6 lg:px-8 pt-16 pb-12">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-10 lg:gap-12">
          {/* Column 1: Logo, Description, Socials */}
          <div className="md:col-span-2 lg:col-span-1">
            <Link to="/" className="inline-block mb-4">
              <h stijl="text-3xl font-bold text-indigo-400 tracking-tight hover:text-indigo-300 transition-colors">
                AucHub
              </h>
            </Link>
            <p className="text-sm text-slate-400 mb-6 leading-relaxed">
              Your premier global marketplace for discovering unique treasures and engaging in thrilling online auctions. Bid, win, and redefine your collection.
            </p>
            <div className="flex space-x-4">
              {socialLinks.map((social) => (
                <a
                  key={social.label}
                  href={social.href}
                  target="_blank"
                  rel="noopener noreferrer"
                  aria-label={social.label}
                  className="text-slate-400 hover:text-indigo-400 transition-colors duration-200 bg-slate-800/50 hover:bg-slate-700/70 p-2.5 rounded-full"
                >
                  <social.icon size={16} />
                </a>
              ))}
            </div>
          </div>

          {/* Column 2: Explore Links */}
          <div>
            <h3 className="text-md font-semibold text-slate-100 mb-5 uppercase tracking-wider">
              Explore
            </h3>
            <ul className="space-y-3">
              <li><Link to="/about" className="text-sm hover:text-indigo-400 transition-colors">About Us</Link></li>
              <li><Link to="/how-it-works" className="text-sm hover:text-indigo-400 transition-colors">How It Works</Link></li>
              <li><Link to="/categories" className="text-sm hover:text-indigo-400 transition-colors">Categories</Link></li>
              <li><Link to="/live-auctions" className="text-sm hover:text-indigo-400 transition-colors">Live Auctions</Link></li>
              <li><Link to="/seller-application" className="text-sm hover:text-indigo-400 transition-colors">Become a Seller</Link></li>
            </ul>
          </div>

          {/* Column 3: Support Links */}
          <div>
            <h3 className="text-md font-semibold text-slate-100 mb-5 uppercase tracking-wider">
              Support
            </h3>
            <ul className="space-y-3">
              <li><Link to="/contact" className="text-sm hover:text-indigo-400 transition-colors">Contact Us</Link></li>
              <li><Link to="/faq" className="text-sm hover:text-indigo-400 transition-colors">FAQ & Help</Link></li>
              <li><Link to="/shipping-returns" className="text-sm hover:text-indigo-400 transition-colors">Shipping & Returns</Link></li>
              <li><Link to="/trust-safety" className="text-sm hover:text-indigo-400 transition-colors">Trust & Safety</Link></li>
              <li><Link to="/site-status" className="text-sm hover:text-indigo-400 transition-colors">Site Status</Link></li>
            </ul>
          </div>

          {/* Column 4: Contact Info */}
          <div>
            <h3 className="text-md font-semibold text-slate-100 mb-5 uppercase tracking-wider">
              Get in Touch
            </h3>
            <ul className="space-y-4 text-sm">
              <li className="flex items-start space-x-3">
                <FaMapMarkerAlt className="text-indigo-400 mt-1 flex-shrink-0" size={16} />
                <span className="text-slate-400">123 Auction Avenue, Suite 4B<br />Metropolis, MC 98765</span>
              </li>
              <li className="flex items-center space-x-3">
                <FaEnvelope className="text-indigo-400 flex-shrink-0" size={16} />
                <a href="mailto:support@auchub.world" className="hover:text-indigo-400 transition-colors text-slate-400">support@auchub.world</a>
              </li>
              <li className="flex items-center space-x-3">
                <FaPhoneAlt className="text-indigo-400 flex-shrink-0" size={15} />
                <a href="tel:+15551234567" className="hover:text-indigo-400 transition-colors text-slate-400">(555) 123-4567</a>
              </li>
            </ul>
            {/* Optional: Newsletter Signup */}
            <form className="mt-8">
              <label htmlFor="footer-newsletter" className="block text-xs font-medium text-slate-200 mb-2">Stay Updated</label>
              <div className="flex">
                <input
                  type="email"
                  id="footer-newsletter"
                  placeholder="Enter your email"
                  className="w-full bg-slate-800 text-slate-200 text-sm px-4 py-2.5 border border-slate-700 rounded-l-md focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 focus:outline-none placeholder-slate-500"
                />
                <button
                  type="submit"
                  className="bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-semibold px-4 py-2.5 rounded-r-md transition-colors"
                >
                  Subscribe
                </button>
              </div>
            </form>
          </div>
        </div>

        {/* Bottom Bar: Copyright */}
        <div className="mt-12 pt-8 border-t border-slate-700/70 text-center">
          <p className="text-xs text-slate-500">
            &copy; {currentYear} AucHub Technologies Inc. All Rights Reserved.
            <Link to="/privacy-policy" className="ml-2 hover:text-indigo-400 transition-colors">Privacy Policy</Link>
            <span className="mx-1 text-slate-600">&bull;</span>
            <Link to="/terms-of-service" className="hover:text-indigo-400 transition-colors">Terms of Service</Link>
          </p>
          <p className="text-xs text-slate-600 mt-2">
            Crafted with passion for the auction community.
          </p>
        </div>
      </div>
    </footer>
  );
}

export default Footer;